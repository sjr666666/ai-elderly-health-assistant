package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 短信通知DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsNotificationDTO {

    /**
     * 通知记录ID
     */
    private Long id;

    /**
     * 监护人ID
     */
    private Long guardianId;

    /**
     * 老人ID
     */
    private Long elderId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 通知内容
     */
    private String message;

    /**
     * 接收手机号
     */
    private String phone;

    /**
     * 发送状态
     */
    private String sendStatus;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
