package com.example.backend.model.enums;

/**
 * 严重程度枚举
 * low（低）/ medium（中）/ high（高）/ critical（严重）/ urgent（紧急）/ warning（警告）
 *
 * @author backend
 * @since 1.0.0
 */
public enum Severity {

    LOW("low", "低"),
    MEDIUM("medium", "中"),
    HIGH("high", "高"),
    CRITICAL("critical", "严重"),
    URGENT("urgent", "紧急"),
    WARNING("warning", "警告");

    private final String code;
    private final String description;

    Severity(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Severity fromCode(String code) {
        if (code == null) {
            return MEDIUM;
        }
        for (Severity severity : values()) {
            if (severity.code.equals(code)) {
                return severity;
            }
        }
        return MEDIUM;
    }
}
