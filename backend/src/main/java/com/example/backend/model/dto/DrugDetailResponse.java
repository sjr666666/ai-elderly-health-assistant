package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 药品详细信息响应 DTO
 * 包含药品成分、适应症、用法用量、注意事项、不良反应等核心信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugDetailResponse {

    /**
     * 药品ID
     */
    private Long id;

    /**
     * 国药准字
     */
    private String approvalNumber;

    /**
     * 通用名
     */
    private String genericName;

    /**
     * 商品名
     */
    private String tradeName;

    /**
     * 俗名/别名
     */
    private String commonName;

    /**
     * 规格
     */
    private String specification;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 药品分类
     */
    private String category;

    /**
     * 药品成分
     */
    private String ingredient;

    /**
     * 适应症
     */
    private String indications;

    /**
     * 用法用量
     */
    private String usage;

    /**
     * 注意事项
     */
    private String precautions;

    /**
     * 不良反应
     */
    private String adverseReactions;

    /**
     * 药品说明原文
     */
    private String description;

    /**
     * 药品图片URL
     */
    private String imageUrl;
}