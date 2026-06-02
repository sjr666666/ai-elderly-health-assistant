package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单个药品冲突检测结果 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugConflictResult {

    /**
     * 冲突的药品A名称
     */
    private String drugA;

    /**
     * 冲突的药品B名称（可能是药品、食物、饮料或保健品）
     */
    private String drugB;

    /**
     * 冲突类型
     * DRUG_DRUG: 药品-药品冲突
     * DRUG_FOOD: 药品-食物冲突
     * DRUG_BEVERAGE: 药品-饮料冲突
     * DRUG_SUPPLEMENT: 药品-保健品冲突
     */
    private ConflictType conflictType;

    /**
     * 冲突严重程度
     * SEVERE: 重度（禁止同时使用）
     * MODERATE: 中度（谨慎使用）
     * MILD: 轻度（可以使用，但需注意）
     * NONE: 无冲突
     */
    private SeverityLevel severity;

    /**
     * 冲突原理（专业描述）
     */
    private String conflictMechanism;

    /**
     * 冲突原理（通俗描述）
     */
    private String conflictExplanation;

    /**
     * 风险提示
     */
    private String riskWarning;

    /**
     * 替代方案建议
     */
    private List<String> alternatives;

    /**
     * 冲突类型枚举
     */
    public enum ConflictType {
        DRUG_DRUG("药品-药品冲突"),
        DRUG_FOOD("药品-食物冲突"),
        DRUG_BEVERAGE("药品-饮料冲突"),
        DRUG_SUPPLEMENT("药品-保健品冲突");

        private final String description;

        ConflictType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 严重程度枚举
     */
    public enum SeverityLevel {
        SEVERE("重度", "禁止同时使用", "#dc2626"),
        MODERATE("中度", "谨慎使用", "#ea580c"),
        MILD("轻度", "可以使用，但需注意", "#ca8a04"),
        NONE("无冲突", "安全使用", "#16a34a");

        private final String description;
        private final String action;
        private final String color;

        SeverityLevel(String description, String action, String color) {
            this.description = description;
            this.action = action;
            this.color = color;
        }

        public String getDescription() {
            return description;
        }

        public String getAction() {
            return action;
        }

        public String getColor() {
            return color;
        }

        public static SeverityLevel fromString(String value) {
            for (SeverityLevel level : values()) {
                if (level.name().equalsIgnoreCase(value) || 
                    level.description.equalsIgnoreCase(value)) {
                    return level;
                }
            }
            return NONE;
        }
    }
}