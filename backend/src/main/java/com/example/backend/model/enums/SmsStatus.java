package com.example.backend.model.enums;

/**
 * 短信发送状态枚举
 * pending（待发送）/ sent（已发送）/ failed（发送失败）
 *
 * @author backend
 * @since 1.0.0
 */
public enum SmsStatus {

    PENDING("pending", "待发送"),
    SENT("sent", "已发送"),
    FAILED("failed", "发送失败");

    private final String code;
    private final String description;

    SmsStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SmsStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (SmsStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
