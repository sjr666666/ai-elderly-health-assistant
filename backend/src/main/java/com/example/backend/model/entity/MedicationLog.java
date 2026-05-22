package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 服药确认记录实体类
 * 对应数据库表：medication_log
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medication_log")
public class MedicationLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 计划明细ID
     * 关联medication_plan表的主键
     */
    @TableField("plan_id")
    private Long planId;

    /**
     * 老人ID
     * 关联sys_user表的主键
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 状态
     * taken（已服用）/ missed（已错过）/ skipped（已跳过）
     */
    @TableField("status")
    private String status;

    /**
     * 确认时间
     * 用户确认服药的时间
     */
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * 备注
     * 用户添加的备注信息
     */
    @TableField("note")
    private String note;

    /**
     * 服药状态枚举
     */
    public enum Status {
        TAKEN("taken", "已服用"),
        MISSED("missed", "已错过"),
        SKIPPED("skipped", "已跳过");

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
            return MISSED;
        }
    }
}