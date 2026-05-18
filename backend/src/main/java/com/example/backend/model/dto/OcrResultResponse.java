package com.example.backend.model.dto;

import com.example.backend.model.entity.OcrRecord;

import java.math.BigDecimal;

public class OcrResultResponse {

    private Long id;
    private Long userId;
    private String imageUrl;
    private String rawText;
    private Long matchedDrugId;
    private String matchedDrugName;
    private BigDecimal matchScore;
    private String status;

    public static OcrResultResponse fromEntity(OcrRecord record) {
        OcrResultResponse response = new OcrResultResponse();
        response.setId(record.getId());
        response.setUserId(record.getUserId());
        response.setImageUrl(record.getImageUrl());
        response.setRawText(record.getRawText());
        response.setMatchedDrugId(record.getMatchedDrugId());
        response.setMatchScore(record.getMatchScore());
        response.setStatus(record.getStatus());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
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

    public BigDecimal getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(BigDecimal matchScore) {
        this.matchScore = matchScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}