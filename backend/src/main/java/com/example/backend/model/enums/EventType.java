package com.example.backend.model.enums;

/**
 * 事件类型枚举
 * sos（紧急求助）/ missed_dose（漏服药品）/ expiry_warning（药品过期）/
 * fall_detected（跌倒检测）/ bind_request（绑定请求）
 *
 * @author backend
 * @since 1.0.0
 */
public enum EventType {

    SOS("sos", "紧急求助"),
    MISSED_DOSE("missed_dose", "漏服药品"),
    EXPIRY_WARNING("expiry_warning", "药品过期"),
    FALL_DETECTED("fall_detected", "跌倒检测"),
    BIND_REQUEST("bind_request", "绑定请求");

    private final String code;
    private final String description;

    EventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static EventType fromCode(String code) {
        if (code == null) {
            return SOS;
        }
        for (EventType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return SOS;
    }
}
