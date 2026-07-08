package com.example.backend.model.enums;

/**
 * 事件处理状态枚举
 * 用于紧急事件的 resolved/pending 状态标识
 */
public enum EventResolveStatus {

    PENDING("pending", "待处理"),
    RESOLVED("resolved", "已处理");

    private final String code;
    private final String description;

    EventResolveStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static EventResolveStatus fromCode(String code) {
        for (EventResolveStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
