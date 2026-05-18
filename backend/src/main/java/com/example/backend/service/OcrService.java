package com.example.backend.service;

import com.example.backend.model.dto.OcrResultResponse;
import com.example.backend.model.dto.OcrUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    OcrUploadResponse uploadAndRecognize(MultipartFile file, Long userId);

    OcrResultResponse getOcrResult(String taskId);
}