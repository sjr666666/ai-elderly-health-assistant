package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 老人端通知实体类
 * 对应数据库表：elder_notification
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("elder_notification")
public class ElderNotification extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 老人ID
     */
    @TableField("elder_id")
    private Long elderId;

    /**
     * 通知类型：bind_request/system/medication_reminder
     */
    @TableField("notification_type")
    private String notificationType;

    /**
     * 通知标题
     */
    @TableField("title")
    private String title;

    /**
     * 通知内容
     */
    @TableField("content")
    private String content;

    /**
     * 附加数据（JSON格式）
     */
    @TableField("extra_data")
    private String extraData;

    /**
     * 是否已读：0-未读 1-已读
     */
    @TableField("is_read")
    @JsonProperty("isRead")
    private Integer isRead;

    /**
     * 是否已处理：0-未处理 1-已处理
     */
    @TableField("is_handled")
    @JsonProperty("isHandled")
    private Integer isHandled;
}
