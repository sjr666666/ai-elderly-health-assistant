package com.example.backend.model.dto;

import lombok.Data;

@Data
public class MedicationRecordItemDTO {
    private Long planId;
    private Long drugId;
    private String drugName;
    private String dosageAtTime;
    private String timeSlot;
    private String timeSlotLabel;
    private String status;
    private Boolean deleted;
    private String boxDrugName;
}