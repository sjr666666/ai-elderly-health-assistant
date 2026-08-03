package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.backend.model.enums.SmsStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 短信通知记录实体类
 * 对应数据库表：sms_notification_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sms_notification_log")
public class SmsNotificationLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 监护人ID
     */
    @TableField("guardian_id")
    private Long guardianId;

    /**
     * 老人ID
     */
    @TableField("elder_id")
    private Long elderId;

    /**
     * 接收手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 短信类型
     */
    @TableField("sms_type")
    private String smsType;

    /**
     * 短信内容
     */
    @TableField("content")
    private String content;

    /**
     * 发送状态
     * @see SmsStatus
     */
    @TableField("status")
    private String status;

    /**
     * 服务商消息ID
     */
    @TableField("provider_msg_id")
    private String providerMsgId;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 发送时间
     */
    @TableField("sent_at")
    private LocalDateTime sentAt;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 是否已读：0-未读 1-已读
     */
    @TableField("is_read")
    private Integer isRead;
}
