package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 药品基础信息响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugInfoResponse {

    /**
     * 药品ID
     */
    private Long id;

    /**
     * 药品名称
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
     * 显示文本（用于下拉列表展示）
     * 格式："药品名称 (规格) - 生产厂家"
     */
    private String displayText;
}
