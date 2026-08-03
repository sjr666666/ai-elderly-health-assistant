package com.example.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 老人端通知DTO
 */
@Data
@Builder
public class ElderNotificationDTO {

    private Long id;

    private Long elderId;

    private String notificationType;

    private String title;

    private String content;

    private String extraData;

    @JsonProperty("isRead")
    private Integer isRead;

    @JsonProperty("isHandled")
    private Integer isHandled;

    private LocalDateTime createdAt;
}
