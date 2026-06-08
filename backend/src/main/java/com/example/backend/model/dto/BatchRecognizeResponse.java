package com.example.backend.model.dto;

import java.util.List;

/**
 * 批量药品识别响应DTO
 */
public class BatchRecognizeResponse {

    private String batchId;
    private List<RecognizeItem> items;
    private int totalCount;
    private int successCount;
    private int failedCount;

    public static class RecognizeItem {
        private String imageId;
        private String imageUrl;
        private String status;
        private Long matchedDrugId;
        private String matchedDrugName;
        private String matchedDrugSpec;
        private String rawText;
        private Double matchScore;
        private String message;

        public String getImageId() {
            return imageId;
        }

        public void setImageId(String imageId) {
            this.imageId = imageId;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getMatchedDrugId() {
            return matchedDrugId;
        }

        public void setMatchedDrugId(Long matchedDrugId) {
            this.matchedDrugId = matchedDrugId;
        }

        public String getMatchedDrugName() {
            return matchedDrugName;
        }

        public void setMatchedDrugName(String matchedDrugName) {
            this.matchedDrugName = matchedDrugName;
        }

        public String getMatchedDrugSpec() {
            return matchedDrugSpec;
        }

        public void setMatchedDrugSpec(String matchedDrugSpec) {
            this.matchedDrugSpec = matchedDrugSpec;
        }

        public String getRawText() {
            return rawText;
        }

        public void setRawText(String rawText) {
            this.rawText = rawText;
        }

        public Double getMatchScore() {
            return matchScore;
        }

        public void setMatchScore(Double matchScore) {
            this.matchScore = matchScore;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public List<RecognizeItem> getItems() {
        return items;
    }

    public void setItems(List<RecognizeItem> items) {
        this.items = items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
}
