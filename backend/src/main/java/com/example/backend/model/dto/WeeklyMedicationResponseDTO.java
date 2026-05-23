package com.example.backend.model.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class WeeklyMedicationResponseDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<DailyMedicationDTO> dailyRecords;
}