package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 今日一课 前端展示DTO
 *
 * @author backend
 * @since 1.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyLessonDTO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 推送日期 (yyyy-MM-dd)
     */
    private String lessonDate;

    /**
     * 本次科普针对的慢病名称
     */
    private String chronicDisease;

    /**
     * 科普标题
     */
    private String title;

    /**
     * 科普正文
     */
    private String content;

    /**
     * 是否已生成
     */
    private boolean generated;

    /**
     * 错误信息（生成失败时）
     */
    private String errorMsg;
}
