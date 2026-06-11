package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 紧急事件实体类
 * 对应数据库表：emergency_event
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("emergency_event")
public class EmergencyEvent extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 老人ID
     */
    @TableField("elder_id")
    private Long elderId;

    /**
     * 事件类型
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 严重程度：low/medium/high
     */
    @TableField("severity")
    private String severity;

    /**
     * 事件描述
     */
    @TableField("description")
    private String description;

    /**
     * 事件发生时间
     */
    @TableField("event_time")
    private LocalDateTime eventTime;

    /**
     * 是否已处理：0-未处理 1-已处理
     */
    @TableField("is_resolved")
    private Integer isResolved;

    /**
     * 处理人ID
     */
    @TableField("resolved_by")
    private Long resolvedBy;

    /**
     * 处理时间
     */
    @TableField("resolved_at")
    private LocalDateTime resolvedAt;
}
