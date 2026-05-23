package com.example.backend.model.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class DailyMedicationDTO {
    private LocalDate date;
    private List<MedicationRecordItemDTO> items;
}