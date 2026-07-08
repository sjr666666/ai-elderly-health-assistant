package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.util.SnowflakeIdGenerator;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.OcrRecordMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.BatchRecognizeResponse;
import com.example.backend.model.dto.OcrResultResponse;
import com.example.backend.model.dto.OcrUploadResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.OcrRecord;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.DrugRecognitionService;
import com.example.backend.service.OcrAsyncService;
import com.example.backend.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class OcrServiceImpl implements OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrServiceImpl.class);

    private final OcrRecordMapper ocrRecordMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final UserMapper userMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final OcrAsyncService ocrAsyncService;
    private final DrugRecognitionService drugRecognitionService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    /**
     * 批量识别结果缓存的过期时间（毫秒），默认 30 分钟。
     */
    @Value("${ocr.batch-cache.ttl-millis:1800000}")
    private long batchCacheTtlMillis;

    /**
     * 过期缓存清理任务的执行间隔（毫秒），默认 5 分钟。
     */
    @Value("${ocr.batch-cache.cleanup-interval-millis:300000}")
    private long batchCacheCleanupIntervalMillis;

    // 批量识别结果缓存（用于轮询获取结果），value 中携带写入时间，用于过期淘汰
    private static final ConcurrentHashMap<String, CacheEntry> batchResultCache = new ConcurrentHashMap<>();

    private ScheduledExecutorService batchCacheCleaner;

    @PostConstruct
    public void initBatchCacheCleaner() {
        this.batchCacheCleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ocr-batch-cache-cleaner");
            t.setDaemon(true);
            return t;
        });
        long interval = Math.max(60_000L, batchCacheCleanupIntervalMillis);
        this.batchCacheCleaner.scheduleAtFixedRate(this::evictExpiredBatchResults,
                interval, interval, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdownBatchCacheCleaner() {
        if (batchCacheCleaner != null) {
            batchCacheCleaner.shutdownNow();
        }
    }

    private void evictExpiredBatchResults() {
        long now = System.currentTimeMillis();
        long ttl = batchCacheTtlMillis;
        Iterator<Map.Entry<String, CacheEntry>> it = batchResultCache.entrySet().iterator();
        int removed = 0;
        try {
            while (it.hasNext()) {
                Map.Entry<String, CacheEntry> e = it.next();
                if (e.getValue() == null || e.getValue().isExpired(now, ttl)) {
                    it.remove();
                    removed++;
                }
            }
        } catch (Exception ex) {
            logger.warn("清理批量识别结果缓存时发生异常", ex);
        }
        if (removed > 0) {
            logger.debug("已清理 {} 条过期的批量识别结果缓存", removed);
        }
    }

    /**
     * 缓存条目，记录写入时间用于 TTL 判定。
     */
    private static final class CacheEntry {
        final long createdAt;
        final BatchRecognizeResponse response;

        CacheEntry(BatchRecognizeResponse response) {
            this.createdAt = System.currentTimeMillis();
            this.response = response;
        }

        boolean isExpired(long now, long ttlMillis) {
            return now - createdAt >= ttlMillis;
        }
    }

    @Autowired
    public OcrServiceImpl(
            OcrRecordMapper ocrRecordMapper,
            DrugBaseMapper drugBaseMapper,
            UserMapper userMapper,
            OcrAsyncService ocrAsyncService,
            DrugRecognitionService drugRecognitionService) {
        this.ocrRecordMapper = ocrRecordMapper;
        this.drugBaseMapper = drugBaseMapper;
        this.userMapper = userMapper;
        this.snowflakeIdGenerator = new SnowflakeIdGenerator(1, 1);
        this.ocrAsyncService = ocrAsyncService;
        this.drugRecognitionService = drugRecognitionService;
    }

    @Override
    public OcrUploadResponse uploadAndRecognize(MultipartFile file, Long userId) {
        try {
            // 入口校验：文件类型+大小，杜绝无效请求
            validateImageFile(file);

            String fileId = String.valueOf(snowflakeIdGenerator.nextId());
            logger.info("开始处理OCR识别任务 - fileId: {}, userId (雪花算法ID): {}, fileName: {}",
                    fileId, userId, file.getOriginalFilename());

            // 根据雪花算法 user_id 查询实际的自增主键 id
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysUser::getId, userId);
            SysUser user = userMapper.selectOne(queryWrapper);

            if (user == null) {
                logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
                throw new RuntimeException("用户不存在");
            }

            Long actualUserId = user.getId();
            logger.info("用户ID转换成功 - 雪花算法ID: {}, 自增主键ID: {}", userId, actualUserId);

            String imageUrl = saveFileLocally(file, fileId);

            OcrRecord ocrRecord = new OcrRecord();
            ocrRecord.setUserId(actualUserId);  // 使用自增主键ID
            ocrRecord.setImageUrl(imageUrl);
            ocrRecord.setStatus(OcrRecord.Status.PENDING.getCode());
            ocrRecordMapper.insert(ocrRecord);

            Long dbRecordId = ocrRecord.getId();
            logger.info("OCR记录已创建 - dbRecordId: {}, imageUrl: {}", dbRecordId, imageUrl);

            // 调用异步服务处理OCR识别
            ocrAsyncService.processOcrAsync(dbRecordId);
            logger.info("异步OCR任务已提交 - dbRecordId: {}", dbRecordId);

            return OcrUploadResponse.builder()
                    .taskId(String.valueOf(dbRecordId))
                    .status(OcrRecord.Status.PENDING.getCode())
                    .build();

        } catch (Exception e) {
            logger.error("OCR上传失败", e);
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    private String saveFileLocally(MultipartFile file, String fileId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 调试：检查文件信息
        logger.info("文件信息 - originalFilename: {}, contentType: {}, size: {}", 
                originalFilename, file.getContentType(), file.getSize());

        // 自动创建上传目录（如果不存在）
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
            logger.info("自动创建上传目录: {}", uploadDir.toAbsolutePath());
        }

        String fileName = fileId + extension;
        Path filePath = uploadDir.resolve(fileName);

        // 删除已存在的文件
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // 先保存文件
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath);
        }

        // 调试：检查保存后的文件
        if (Files.exists(filePath)) {
            byte[] savedData = Files.readAllBytes(filePath);
            logger.info("保存后的文件大小: {}, 文件头: {}", 
                    savedData.length,
                    savedData.length >= 4 ? String.format("%02X-%02X-%02X-%02X", 
                            savedData[0], savedData[1], savedData[2], savedData[3]) : "空");
        }

        String imageUrl = filePath.toAbsolutePath().toString();
        logger.info("图片已保存到本地 - fileId: {}, path: {}", fileId, imageUrl);
        return imageUrl;
    }

    

    @Override
    public OcrResultResponse getOcrResult(String taskId) {
        try {
            Long recordId = Long.parseLong(taskId);
            OcrRecord ocrRecord = ocrRecordMapper.selectById(recordId);

            if (ocrRecord == null) {
                return null;
            }

            OcrResultResponse response = OcrResultResponse.fromEntity(ocrRecord);

            if (ocrRecord.getMatchedDrugId() != null) {
                DrugBase drug = drugBaseMapper.selectById(ocrRecord.getMatchedDrugId());
                if (drug != null) {
                    response.setMatchedDrugName(drug.getGenericName());
                }
            }

            return response;

        } catch (Exception e) {
            logger.error("查询OCR结果失败 - taskId: {}", taskId, e);
            return null;
        }
    }

    @Override
    public BatchRecognizeResponse batchUploadAndRecognize(MultipartFile[] files, Long userId) {
        String batchId = String.valueOf(snowflakeIdGenerator.nextId());
        logger.info("开始处理批量OCR识别任务 - batchId: {}, fileCount: {}, userId (雪花算法ID): {}",
                batchId, files.length, userId);

        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getId, userId);
        SysUser user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.error("用户不存在 - userId (雪花算法ID): {}", userId);
            throw new RuntimeException("用户不存在");
        }

        Long actualUserId = user.getId();
        logger.info("用户ID转换成功 - 雪花算法ID: {}, 自增主键ID: {}", userId, actualUserId);

        BatchRecognizeResponse response = new BatchRecognizeResponse();
        response.setBatchId(batchId);
        response.setTotalCount(files.length);

        List<BatchRecognizeResponse.RecognizeItem> items = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        // 同步处理每张图片
        for (MultipartFile file : files) {
            BatchRecognizeResponse.RecognizeItem item = new BatchRecognizeResponse.RecognizeItem();

            try {
                // 入口校验：文件类型+大小
                validateImageFile(file);

                String fileId = String.valueOf(snowflakeIdGenerator.nextId());
                String imageUrl = saveFileLocally(file, fileId);
                item.setImageId(fileId);
                item.setImageUrl(imageUrl);

                // 创建OCR记录
                OcrRecord ocrRecord = new OcrRecord();
                ocrRecord.setUserId(actualUserId);  // 使用自增主键ID
                ocrRecord.setImageUrl(imageUrl);
                ocrRecord.setStatus(OcrRecord.Status.PENDING.getCode());
                ocrRecordMapper.insert(ocrRecord);
                Long recordId = ocrRecord.getId();

                // 调用异步OCR处理（会直接执行完成）
                ocrAsyncService.processOcrAsync(recordId);

                // 等待异步处理完成并获取结果
                OcrRecord processedRecord = waitForProcessingResult(recordId);

                if (processedRecord != null) {
                    item.setStatus(processedRecord.getStatus());
                    item.setRawText(processedRecord.getRawText());

                    if (processedRecord.getMatchedDrugId() != null) {
                        item.setMatchedDrugId(processedRecord.getMatchedDrugId());
                        DrugBase drug = drugBaseMapper.selectById(processedRecord.getMatchedDrugId());
                        if (drug != null) {
                            item.setMatchedDrugName(drug.getGenericName());
                            item.setMatchedDrugSpec(drug.getSpecification());
                        }
                    }

                    if (processedRecord.getMatchScore() != null) {
                        item.setMatchScore(processedRecord.getMatchScore().doubleValue());
                    }

                    if ("matched".equals(processedRecord.getStatus())) {
                        successCount.incrementAndGet();
                        item.setMessage("识别成功");
                    } else {
                        failedCount.incrementAndGet();
                        item.setMessage("未能识别出匹配的药品");
                    }
                } else {
                    failedCount.incrementAndGet();
                    item.setStatus(OcrRecord.Status.FAILED.getCode());
                    item.setMessage("处理超时");
                }

            } catch (Exception e) {
                logger.error("批量图片处理失败", e);
                failedCount.incrementAndGet();
                item.setStatus(OcrRecord.Status.FAILED.getCode());
                item.setMessage("处理失败: " + e.getMessage());
            }

            items.add(item);
        }

        response.setItems(items);
        response.setSuccessCount(successCount.get());
        response.setFailedCount(failedCount.get());

        // 缓存结果供后续查询
        batchResultCache.put(batchId, new CacheEntry(response));

        logger.info("批量OCR识别完成 - batchId: {}, 成功: {}, 失败: {}",
                batchId, successCount.get(), failedCount.get());

        return response;
    }

    private OcrRecord waitForProcessingResult(Long recordId) {
        int maxWaitTime = 30000; // 最多等待30秒
        int checkInterval = 500; // 每500ms检查一次
        int maxChecks = maxWaitTime / checkInterval;

        for (int i = 0; i < maxChecks; i++) {
            try {
                OcrRecord record = ocrRecordMapper.selectById(recordId);
                if (record != null && !OcrRecord.Status.PENDING.getCode().equals(record.getStatus())) {
                    return record;
                }
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    @Override
    public BatchRecognizeResponse getBatchResult(String batchId) {
        CacheEntry entry = batchResultCache.get(batchId);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(System.currentTimeMillis(), batchCacheTtlMillis)) {
            batchResultCache.remove(batchId, entry);
            return null;
        }
        return entry.response;
    }

    /**
     * 校验上传的图片文件：大小、扩展名、内容类型。
     * 不符合则抛 IllegalArgumentException，由上层统一捕获转成 400 响应。
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空，请选择图片后再上传");
        }
        long maxSize = 10L * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("图片大小不能超过 10MB，当前文件：" + (file.getSize() / 1024) + "KB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                    || lower.endsWith(".bmp") || lower.endsWith(".webp"))) {
                throw new IllegalArgumentException("图片格式不支持，请使用 JPG/PNG/BMP/WEBP 格式");
            }
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("请上传图片文件（当前文件类型：" + contentType + "）");
        }
    }
}
