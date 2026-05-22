package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 大模型对话记录实体类
 * 对应数据库表：ai_conversation_log
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation_log")
public class AiConversationLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     * 关联sys_user表的主键
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 查询类型
     * explain（用药说明）/ conflict_check（冲突检查）/ emergency（紧急咨询）
     */
    @TableField("query_type")
    private String queryType;

    /**
     * 用户输入
     * 用户输入的文本或OCR识别结果
     */
    @TableField("user_input")
    private String userInput;

    /**
     * AI返回白话内容
     * 大模型返回的、面向普通用户的白话解释内容
     */
    @TableField("ai_output")
    private String aiOutput;

    /**
     * 是否通过安全检查
     * 1表示通过，0表示未通过，默认值为1
     */
    @TableField("safety_check_passed")
    private Integer safetyCheckPassed;

    /**
     * 时间
     * 对话发生的时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 查询类型枚举
     */
    public enum QueryType {
        EXPLAIN("explain", "用药说明"),
        CONFLICT_CHECK("conflict_check", "冲突检查"),
        EMERGENCY("emergency", "紧急咨询");

        private final String code;
        private final String description;

        QueryType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static QueryType fromCode(String code) {
            for (QueryType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return EXPLAIN;
        }
    }

    /**
     * 判断是否通过安全检查
     * @return true表示通过，false表示未通过
     */
    public boolean isSafetyCheckPassed() {
        return safetyCheckPassed != null && safetyCheckPassed == 1;
    }

    /**
     * 设置是否通过安全检查
     * @param passed true表示通过，false表示未通过
     */
    public void setSafetyCheckPassed(boolean passed) {
        this.safetyCheckPassed = passed ? 1 : 0;
    }
}