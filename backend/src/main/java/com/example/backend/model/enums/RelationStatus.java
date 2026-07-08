package com.example.backend.model.enums;

/**
 * 关系状态枚举
 * active（有效）/ inactive（已解除）
 *
 * @author backend
 * @since 1.0.0
 */
public enum RelationStatus {

    ACTIVE("active", "有效"),
    INACTIVE("inactive", "已解除");

    private final String code;
    private final String description;

    RelationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RelationStatus fromCode(String code) {
        if (code == null) {
            return ACTIVE;
        }
        for (RelationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return ACTIVE;
    }
}
