package com.example.backend.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReminderResponseDTO {
    private Long reminderId;
    private String type;
    private String content;
    private LocalDateTime createdAt;
}