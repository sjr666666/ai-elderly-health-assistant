package com.example.backend.model.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TodayPlanResponseDTO {
    private LocalDate date;
    private List<TodayPlanItemDTO> items;
}