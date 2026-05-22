package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 药品搜索响应DTO
 * 包含匹配度信息，用于智能搜索功能
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugSearchResponse {

    /**
     * 药品ID
     */
    private Long id;

    /**
     * 药品名称（通用名）
     */
    private String drugName;

    /**
     * 规格
     */
    private String specification;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 商品名
     */
    private String tradeName;

    /**
     * 药品类别
     */
    private String category;

    /**
     * 匹配度（0-1）
     */
    private Double matchScore;

    /**
     * 匹配类型：exact（精确匹配）、fuzzy（模糊匹配）、category（类别匹配）、ai（AI识别）
     */
    private String matchType;
}
