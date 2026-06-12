package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 家属端-老人今日用药计划DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElderMedicationPlanDTO {

    /**
     * 老人ID
     */
    private Long elderId;

    /**
     * 老人姓名
     */
    private String elderName;

    /**
     * 今日计划总数
     */
    private Integer totalCount;

    /**
     * 已服用数
     */
    private Integer takenCount;

    /**
     * 待服用数
     */
    private Integer pendingCount;

    /**
     * 漏服数
     */
    private Integer missedCount;

    /**
     * 完成进度百分比（0-100）
     */
    private Integer progressPercent;

    /**
     * 用药计划明细列表
     */
    private List<ElderMedicationPlanItemDTO> items;
}
