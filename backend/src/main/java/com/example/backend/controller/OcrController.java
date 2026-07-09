package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ResponseResult;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.entity.DrugRecognitionLog;
import com.example.backend.model.dto.BatchRecognizeResponse;
import com.example.backend.model.dto.OcrResultResponse;
import com.example.backend.model.dto.OcrUploadResponse;
import com.example.backend.mapper.DrugRecognitionLogMapper;
import com.example.backend.service.OcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drug/recognize")
public class OcrController {

    private static final Logger logger = LoggerFactory.getLogger(OcrController.class);

    private final OcrService ocrService;
    private final UserMapper userMapper;
    private final DrugRecognitionLogMapper recognitionLogMapper;

    @Autowired
    public OcrController(OcrService ocrService, UserMapper userMapper, DrugRecognitionLogMapper recognitionLogMapper) {
        this.ocrService = ocrService;
        this.userMapper = userMapper;
        this.recognitionLogMapper = recognitionLogMapper;
    }

    /**
     * 获取当前认证用户的ID（数据库主键）
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }
        return (Long) authentication.getPrincipal();
    }

    @PostMapping("/upload")
    public ResponseResult<OcrUploadResponse> uploadAndRecognize(
            @RequestParam("file") MultipartFile file) {
        try {
            Long userId = getCurrentUserId();
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

            OcrUploadResponse response = ocrService.uploadAndRecognize(file, userId);

            return ResponseResult.success("图片已上传，识别中", response);

        } catch (Exception e) {
            logger.error("图片上传失败", e);
            return ResponseResult.fail("图片上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-upload")
    public ResponseResult<BatchRecognizeResponse> batchUploadAndRecognize(
            @RequestParam("files") MultipartFile[] files) {
        try {
            Long userId = getCurrentUserId();
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

            BatchRecognizeResponse response = ocrService.batchUploadAndRecognize(files, userId);

            return ResponseResult.success("批量图片已上传，识别中", response);

        } catch (Exception e) {
            logger.error("批量图片上传失败", e);
            return ResponseResult.fail("批量图片上传失败: " + e.getMessage());
        }
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

    @GetMapping("/history")
    public ResponseResult<List<DrugRecognitionLog>> getRecognitionHistory(
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        try {
            Long userId = getCurrentUserId();
            int safeLimit = Math.min(limit, 50);

            QueryWrapper<DrugRecognitionLog> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId)
                    .orderByDesc("created_at");

            Page<DrugRecognitionLog> page = new Page<>(1, safeLimit, false);
            Page<DrugRecognitionLog> pageResult = recognitionLogMapper.selectPage(page, queryWrapper);
            return ResponseResult.success(pageResult.getRecords());

        } catch (Exception e) {
            logger.error("查询识别历史失败", e);
            return ResponseResult.fail("查询识别历史失败: " + e.getMessage());
        }
    }
}
