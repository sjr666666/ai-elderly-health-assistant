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
    BIND_REQUEST("bind_request", "绑定请求"),
    PHONE_UPDATE("phone_update", "电话变更"),
    EMERGENCY_ALERT("emergency_alert", "紧急报警"),
    MEDICATION_REMINDER("medication_reminder", "用药提醒"),
    EXPIRING_DRUG("expiring_drug", "药品临期"),
    EXPIRING_DRUG_REMINDER("expiring_drug_reminder", "药品临期提醒"),
    MEDICATION_MISSED("medication_missed", "漏服药物");

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
