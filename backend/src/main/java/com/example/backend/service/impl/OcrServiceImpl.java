package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.util.SnowflakeIdGenerator;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.OcrRecordMapper;
import com.example.backend.model.dto.BatchRecognizeResponse;
import com.example.backend.model.dto.OcrResultResponse;
import com.example.backend.model.dto.OcrUploadResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.OcrRecord;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OcrServiceImpl implements OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrServiceImpl.class);

    private final OcrRecordMapper ocrRecordMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final OcrAsyncService ocrAsyncService;
    private final DrugRecognitionService drugRecognitionService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    // 批量识别结果缓存（用于轮询获取结果）
    private static final ConcurrentHashMap<String, BatchRecognizeResponse> batchResultCache = new ConcurrentHashMap<>();

    @Autowired
    public OcrServiceImpl(
            OcrRecordMapper ocrRecordMapper,
            DrugBaseMapper drugBaseMapper,
            OcrAsyncService ocrAsyncService,
            DrugRecognitionService drugRecognitionService) {
        this.ocrRecordMapper = ocrRecordMapper;
        this.drugBaseMapper = drugBaseMapper;
        this.snowflakeIdGenerator = new SnowflakeIdGenerator(1, 1);
        this.ocrAsyncService = ocrAsyncService;
        this.drugRecognitionService = drugRecognitionService;
    }

    @Override
    public OcrUploadResponse uploadAndRecognize(MultipartFile file, Long userId) {
        try {
            String fileId = String.valueOf(snowflakeIdGenerator.nextId());
            logger.info("开始处理OCR识别任务 - fileId: {}, userId: {}, fileName: {}",
                    fileId, userId, file.getOriginalFilename());

            String imageUrl = saveFileLocally(file, fileId);

            OcrRecord ocrRecord = new OcrRecord();
            ocrRecord.setUserId(userId);
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
        logger.info("开始处理批量OCR识别任务 - batchId: {}, fileCount: {}, userId: {}",
                batchId, files.length, userId);

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
                String fileId = String.valueOf(snowflakeIdGenerator.nextId());
                String imageUrl = saveFileLocally(file, fileId);
                item.setImageId(fileId);
                item.setImageUrl(imageUrl);

                // 创建OCR记录
                OcrRecord ocrRecord = new OcrRecord();
                ocrRecord.setUserId(userId);
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
                    item.setStatus("failed");
                    item.setMessage("处理超时");
                }

            } catch (Exception e) {
                logger.error("批量图片处理失败", e);
                failedCount.incrementAndGet();
                item.setStatus("failed");
                item.setMessage("处理失败: " + e.getMessage());
            }

            items.add(item);
        }

        response.setItems(items);
        response.setSuccessCount(successCount.get());
        response.setFailedCount(failedCount.get());

        // 缓存结果供后续查询
        batchResultCache.put(batchId, response);

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
        return batchResultCache.get(batchId);
    }
}