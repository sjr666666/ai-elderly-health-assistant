package com.example.backend.model.dto;

import lombok.Data;

@Data
public class TodayPlanItemDTO {
    private Long planId;
    private Long drugId;
    private String drugName;
    private String dosageAtTime;
    private String timeSlot;
    private String timeSlotLabel;
    private String status; // pending/taken/skipped
    private Integer remindBefore;

    /**
     * 药箱条目ID（用于更新剩余数量）
     */
    private Long boxItemId;

    /**
     * 当前剩余数量
     */
    private Integer remainingQuantity;

    /**
     * 药箱中的药品名称（商品名/俗名）
     */
    private String boxDrugName;
}