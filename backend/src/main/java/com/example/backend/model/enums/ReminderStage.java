package com.example.backend.model.enums;

/**
 * 提醒阶段枚举
 * none（未触发）/ pre_remind（提前提醒）/ due_now（到时提醒）/
 * overdue（超时提醒）/ notify_family（已通知家属）
 *
 * @author backend
 * @since 1.0.0
 */
public enum ReminderStage {

    NONE("none", "未触发"),
    PRE_REMIND("pre_remind", "提前提醒"),
    DUE_NOW("due_now", "到时提醒"),
    OVERDUE("overdue", "超时提醒"),
    NOTIFY_FAMILY("notify_family", "已通知家属");

    private final String code;
    private final String description;

    ReminderStage(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ReminderStage fromCode(String code) {
        if (code == null) {
            return NONE;
        }
        for (ReminderStage stage : values()) {
            if (stage.code.equals(code)) {
                return stage;
            }
        }
        return NONE;
    }
}
