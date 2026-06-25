package com.example.backend.service;

/**
 * 渐进式提醒服务接口
 * 提醒阶段：none → pre_remind(提前15min) → due_now(到时) → overdue(超时10min) → notify_family(通知家属)
 */
public interface ProgressiveReminderService {

    /**
     * 扫描并推进所有用药计划的提醒阶段
     * 由定时任务每分钟调用
     */
    void processProgressiveReminders();
}
