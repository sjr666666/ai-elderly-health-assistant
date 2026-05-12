package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 提醒通知记录实体类
 * 对应数据库表：reminder_log
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reminder_log")
public class ReminderLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 目标老人ID
     * 关联sys_user表的主键
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 关联计划ID
     * 关联medication_plan表的主键，可为空表示非周期提醒
     */
    @TableField("plan_id")
    private Long planId;

    /**
     * 提醒类型
     * dosage_remind（用药提醒）/ expiry_warning（有效期警告）/ missed_alert（错过提醒）
     */
    @TableField("remind_type")
    private String remindType;

    /**
     * 提醒内容文本
     * 提醒消息的具体内容
     */
    @TableField("content")
    private String content;

    /**
     * 渠道
     * browser_notify（浏览器通知）/ page_popup（页面弹窗）/ email（邮件）/ sms（短信）
     */
    @TableField("channel")
    private String channel;

    /**
     * 状态
     * sent（已发送）/ read（已读）/ failed（发送失败）
     * 默认值：sent
     */
    @TableField("status")
    private String status;

    /**
     * 发送时间
     * 提醒发送的时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 提醒类型枚举
     */
    public enum RemindType {
        DOSAGE_REMIND("dosage_remind", "用药提醒"),
        EXPIRY_WARNING("expiry_warning", "有效期警告"),
        MISSED_ALERT("missed_alert", "错过提醒");

        private final String code;
        private final String description;

        RemindType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static RemindType fromCode(String code) {
            for (RemindType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return DOSAGE_REMIND;
        }
    }

    /**
     * 通知渠道枚举
     */
    public enum Channel {
        BROWSER_NOTIFY("browser_notify", "浏览器通知"),
        PAGE_POPUP("page_popup", "页面弹窗"),
        EMAIL("email", "邮件"),
        SMS("sms", "短信");

        private final String code;
        private final String description;

        Channel(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static Channel fromCode(String code) {
            for (Channel channel : values()) {
                if (channel.code.equals(code)) {
                    return channel;
                }
            }
            return BROWSER_NOTIFY;
        }
    }

    /**
     * 通知状态枚举
     */
    public enum Status {
        SENT("sent", "已发送"),
        READ("read", "已读"),
        FAILED("failed", "发送失败");

        private final String code;
        private final String description;

        Status(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static Status fromCode(String code) {
            for (Status status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            return SENT;
        }
    }
}