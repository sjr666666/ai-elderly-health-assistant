package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 回答质量反馈实体
 * 对应数据库表：rag_feedback
 * 前端「这个回答有用吗」👍/👎 → 落库，供评测集校准与回答质量监控
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rag_feedback")
public class RagFeedback extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 反馈用户ID（sys_user 主键） */
    @TableField("user_id")
    private Long userId;

    /** 用户问题 */
    @TableField("question")
    private String question;

    /** AI 回答全文 */
    @TableField("answer")
    private String answer;

    /** 评分：1=有用（👍），-1=没用（👎） */
    @TableField("rating")
    private Integer rating;

    /** 检索模式（VECTOR/KEYWORD/LOCAL/GUARDED） */
    @TableField("mode")
    private String mode;
}
