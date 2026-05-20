package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.util.SnowflakeIdGenerator;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.OcrRecordMapper;
import com.example.backend.model.dto.OcrResultResponse;
import com.example.backend.model.dto.OcrUploadResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.OcrRecord;
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
import java.util.List;

@Service
public class OcrServiceImpl implements OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrServiceImpl.class);

    private final OcrRecordMapper ocrRecordMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final OcrAsyncService ocrAsyncService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Autowired
    public OcrServiceImpl(
            OcrRecordMapper ocrRecordMapper,
            DrugBaseMapper drugBaseMapper,
            OcrAsyncService ocrAsyncService) {
        this.ocrRecordMapper = ocrRecordMapper;
        this.drugBaseMapper = drugBaseMapper;
        this.snowflakeIdGenerator = new SnowflakeIdGenerator(1, 1);
        this.ocrAsyncService = ocrAsyncService;
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
}