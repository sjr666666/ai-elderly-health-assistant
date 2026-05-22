package com.example.backend.model.dto;

import lombok.Data;

@Data
public class TodayPlanItemDTO {
    private Long planId;
    private String drugName;
    private String dosageAtTime;
    private String timeSlot;
    private String timeSlotLabel;
    private String status; // pending/taken/skipped
    private Integer remindBefore;
}