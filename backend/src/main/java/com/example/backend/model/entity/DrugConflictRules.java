package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 药品冲突规则实体类
 * 对应数据库表：drug_conflict_rules
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("drug_conflict_rules")
public class DrugConflictRules extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 药品A的ID
     * 关联drug_base表的主键
     */
    @TableField("drug_a_id")
    private Long drugAId;

    /**
     * 药品B的ID
     * 关联drug_base表的主键
     */
    @TableField("drug_b_id")
    private Long drugBId;

    /**
     * 冲突等级
     * severe（严重）/ moderate（中等）/ mild（轻微）
     */
    @TableField("conflict_level")
    private String conflictLevel;

    /**
     * 冲突原因（专业描述）
     * 面向专业人士的详细冲突原因说明
     */
    @TableField("conflict_reason")
    private String conflictReason;

    /**
     * 白话版冲突原因
     * 面向普通用户（老人）的简洁易懂的冲突说明
     */
    @TableField("conflict_reason_plain")
    private String conflictReasonPlain;

    /**
     * 数据来源
     * 冲突规则的来源渠道或数据库名称
     */
    @TableField("source")
    private String source;

    /**
     * 冲突等级枚举
     */
    public enum ConflictLevel {
        SEVERE("severe", "严重", "禁止同时服用"),
        MODERATE("moderate", "中等", "谨慎使用"),
        MILD("mild", "轻微", "可以使用，但需注意");

        private final String code;
        private final String description;
        private final String action;

        ConflictLevel(String code, String description, String action) {
            this.code = code;
            this.description = description;
            this.action = action;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public String getAction() {
            return action;
        }

        public static ConflictLevel fromCode(String code) {
            for (ConflictLevel level : values()) {
                if (level.code.equals(code)) {
                    return level;
                }
            }
            return MILD;
        }
    }
}