package com.example.backend.model.dto;

import lombok.Data;

@Data
public class MedicationActionRequest {
    private Long userId;
    private String action; // confirm, skip, undo
}
