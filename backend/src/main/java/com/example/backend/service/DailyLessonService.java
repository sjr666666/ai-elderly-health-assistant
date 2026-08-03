package com.example.backend.service;

import com.example.backend.model.dto.DailyLessonDTO;

import java.util.List;

/**
 * 今日一课 - 慢病科普服务接口
 *
 * @author backend
 * @since 1.1.0
 */
public interface DailyLessonService {

    /**
     * 获取指定用户今日的科普文章（缓存优先）
     * 如果已生成则直接返回缓存，否则调用AI生成
     *
     * @param userId 用户数据库主键ID（sys_user.id）
     * @return 今日科普DTO；用户无慢病史时返回generated=false的提示信息
     */
    DailyLessonDTO getTodayLesson(Long userId);

    /**
     * 强制重新生成指定用户今日的科普文章
     *
     * @param userId 用户数据库主键ID（sys_user.id）
     * @return 新生成的科普DTO
     */
    DailyLessonDTO regenerateTodayLesson(Long userId);

    /**
     * 获取指定用户的科普历史记录（分页）
     *
     * @param userId 用户数据库主键ID（sys_user.id）
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 历史科普列表（按日期倒序）
     */
    List<DailyLessonDTO> getLessonHistory(Long userId, int page, int size);

    /**
     * 定时任务：为所有有慢病史的用户预生成今日科普
     */
    void generateDailyLessons();
}
