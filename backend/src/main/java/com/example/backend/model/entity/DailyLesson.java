package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 今日一课-慢病科普表实体
 * 对应数据库表：daily_lesson
 *
 * @author backend
 * @since 1.1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_lesson")
public class DailyLesson extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（关联sys_user.id）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 推送日期
     */
    @TableField("lesson_date")
    private LocalDate lessonDate;

    /**
     * 本次科普针对的慢病名称
     */
    @TableField("chronic_disease")
    private String chronicDisease;

    /**
     * 科普标题
     */
    @TableField("title")
    private String title;

    /**
     * 科普正文
     */
    @TableField("content")
    private String content;

    /**
     * 是否已生成：0-未生成或失败 1-已生成
     */
    @TableField("is_generated")
    private Integer isGenerated;

    /**
     * AI生成失败时的错误信息
     */
    @TableField("error_msg")
    private String errorMsg;
}
