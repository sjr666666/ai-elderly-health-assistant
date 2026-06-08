package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ResponseResult;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.entity.SysUser;
import com.example.backend.model.dto.BatchRecognizeResponse;
import com.example.backend.model.dto.OcrResultResponse;
import com.example.backend.model.dto.OcrUploadResponse;
import com.example.backend.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/drug/recognize")
public class OcrController {

    private static final Logger logger = LoggerFactory.getLogger(OcrController.class);

    private final OcrService ocrService;
    private final UserMapper userMapper;

    @Autowired
    public OcrController(OcrService ocrService, UserMapper userMapper) {
        this.ocrService = ocrService;
        this.userMapper = userMapper;
    }

    @PostMapping("/upload")
    public ResponseResult<OcrUploadResponse> uploadAndRecognize(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            logger.info("收到图片上传请求 - fileName: {}, size: {}, userId: {}",
                    file.getOriginalFilename(), file.getSize(), userId);

            if (file.isEmpty()) {
                return ResponseResult.fail("图片文件不能为空");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseResult.fail("只能上传图片文件");
            }

            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseResult.fail("图片大小不能超过10MB");
            }

            if (userId == null) {
                userId = 1L;
            }

            Long dbUserId = convertToDbUserId(userId);
            if (dbUserId == null) {
                dbUserId = 1L;
            }

            OcrUploadResponse response = ocrService.uploadAndRecognize(file, dbUserId);

            return ResponseResult.success("图片已上传，识别中", response);

        } catch (Exception e) {
            logger.error("图片上传失败", e);
            return ResponseResult.fail("图片上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-upload")
    public ResponseResult<BatchRecognizeResponse> batchUploadAndRecognize(
            @RequestParam("files") MultipartFile[] files,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            logger.info("收到批量图片上传请求 - fileCount: {}, userId: {}", files.length, userId);

            if (files == null || files.length == 0) {
                return ResponseResult.fail("图片文件不能为空");
            }

            long maxSize = 10 * 1024 * 1024;
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    return ResponseResult.fail("图片文件不能为空");
                }
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseResult.fail("只能上传图片文件");
                }
                if (file.getSize() > maxSize) {
                    return ResponseResult.fail("单张图片大小不能超过10MB");
                }
            }

            if (userId == null) {
                userId = 1L;
            }

            Long dbUserId = convertToDbUserId(userId);
            if (dbUserId == null) {
                dbUserId = 1L;
            }

            BatchRecognizeResponse response = ocrService.batchUploadAndRecognize(files, dbUserId);

            return ResponseResult.success("批量图片已上传，识别中", response);

        } catch (Exception e) {
            logger.error("批量图片上传失败", e);
            return ResponseResult.fail("批量图片上传失败: " + e.getMessage());
        }
    }

    private Long convertToDbUserId(Long snowflakeUserId) {
        try {
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysUser::getUserId, snowflakeUserId);
            SysUser user = userMapper.selectOne(queryWrapper);
            if (user != null) {
                return user.getId();
            }
        } catch (Exception e) {
            logger.error("转换用户ID失败", e);
        }
        return null;
    }

    @GetMapping("/result/{taskId}")
    public ResponseResult<OcrResultResponse> getOcrResult(@PathVariable String taskId) {
        try {
            logger.info("查询 OCR 结果 - taskId: {}", taskId);
            OcrResultResponse result = ocrService.getOcrResult(taskId);

            if (result == null) {
                return ResponseResult.fail("未找到该识别任务");
            }

            return ResponseResult.success(result);

        } catch (Exception e) {
            logger.error("查询 OCR 结果失败", e);
            return ResponseResult.fail("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/batch-result/{batchId}")
    public ResponseResult<BatchRecognizeResponse> getBatchResult(@PathVariable String batchId) {
        try {
            logger.info("查询批量 OCR 结果 - batchId: {}", batchId);
            BatchRecognizeResponse result = ocrService.getBatchResult(batchId);

            if (result == null) {
                return ResponseResult.fail("未找到该批量识别任务");
            }

            return ResponseResult.success(result);

        } catch (Exception e) {
            logger.error("查询批量 OCR 结果失败", e);
            return ResponseResult.fail("查询失败: " + e.getMessage());
        }
    }
}