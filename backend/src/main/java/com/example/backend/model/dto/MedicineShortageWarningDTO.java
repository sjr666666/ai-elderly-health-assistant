package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 缺药预警响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineShortageWarningDTO {

    /**
     * 药箱条目ID
     */
    private Long boxItemId;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 药品名称
     */
    private String drugName;

    /**
     * 药品规格
     */
    private String specification;

    /**
     * 每次用量
     */
    private String dosage;

    /**
     * 服用频率
     */
    private String frequency;

    /**
     * 剩余数量
     */
    private BigDecimal remainingQuantity;

    /**
     * 每日消耗量（解析后的数值，单位：片/粒等）
     */
    private BigDecimal dailyConsumption;

    /**
     * 预计剩余可服用天数
     * null 表示无法计算（数据不完整）
     */
    private Integer remainingDays;

    /**
     * 预警级别
     * critical: 剩余0天（已用尽）
     * urgent: 剩余1-3天
     * warning: 剩余4-6天
     */
    private String warningLevel;

    /**
     * 预警级别描述
     */
    private String warningLevelDesc;
}
