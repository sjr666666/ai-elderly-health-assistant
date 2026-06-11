package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 紧急事件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyEventDTO {

    private Long eventId;
    private Long elderId;
    private String elderName;
    private String eventType;
    private String severity;
    private String description;
    private LocalDateTime eventTime;
    private String status;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
