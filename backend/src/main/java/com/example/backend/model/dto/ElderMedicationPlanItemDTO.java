package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 家属端-老人今日用药计划项DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElderMedicationPlanItemDTO {

    /**
     * 计划ID
     */
    private Long planId;

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
     * 单次剂量
     */
    private String dosageAtTime;

    /**
     * 时段：morning/noon/evening/before_bed
     */
    private String timeSlot;

    /**
     * 时段中文名
     */
    private String timeSlotLabel;

    /**
     * 状态：pending/taken/missed/skipped
     */
    private String status;

    /**
     * 状态中文名
     */
    private String statusLabel;
}
