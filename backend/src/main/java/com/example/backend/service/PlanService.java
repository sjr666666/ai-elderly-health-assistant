package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.model.entity.MedicationPlan;
import com.example.backend.model.dto.ConfirmMedicationResponseDTO;
import com.example.backend.model.dto.TodayPlanResponseDTO;
import com.example.backend.model.dto.ReminderResponseDTO;
import java.util.List;

public interface PlanService extends IService<MedicationPlan> {
    // 原有方法保留不变

    /**
     * 获取今日用药计划
     */
    TodayPlanResponseDTO getTodayPlan();

    /**
     * 确认服药
     */
    ConfirmMedicationResponseDTO confirmMedication(Long planId);

    /**
     * 跳过服药
     */
    void skipMedication(Long planId);

    /**
     * 获取待提醒记录（轮询用）
     */
    List<ReminderResponseDTO> getPendingReminders();

    /**
     * 根据家庭药箱自动生成今日用药计划
     */
    TodayPlanResponseDTO generateDailyPlanFromMedicineBox(Long userId);

    /**
     * 将药箱中的药品添加到用药计划（保存到数据库）
     */
    void addBoxItemToMedicationPlan(Long userId, Long boxItemId, List<String> timeSlots);

    /**
     * 清空用户的所有用药计划（仅用于测试）
     */
    void clearAllPlans(Long userId);
}
