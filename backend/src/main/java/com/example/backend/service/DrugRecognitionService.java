package com.example.backend.service;

import com.example.backend.model.dto.OcrResultResponse;

/**
 * 药品识别服务接口
 * 负责药品识别、名称标准化、匹配和自动入库
 */
public interface DrugRecognitionService {

    /**
     * 处理药品识别结果
     *
     * @param ocrRecordId OCR记录ID
     * @param rawText     识别原始文本
     * @return 识别结果
     */
    OcrResultResponse processRecognition(Long ocrRecordId, String rawText);

    /**
     * 标准化药品名称
     *
     * @param rawName 原始名称
     * @return 标准化后的名称
     */
    String normalizeDrugName(String rawName);

    /**
     * 验证药品名称
     *
     * @param name 药品名称
     * @return 验证结果
     */
    ValidationResult validateDrugName(String name);

    /**
     * 验证结果类
     */
    class ValidationResult {
        private boolean valid;
        private String message;
        private String normalizedName;

        public ValidationResult(boolean valid, String message, String normalizedName) {
            this.valid = valid;
            this.message = message;
            this.normalizedName = normalizedName;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getNormalizedName() {
            return normalizedName;
        }
    }
}