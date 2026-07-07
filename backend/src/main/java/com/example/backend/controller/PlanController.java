package com.example.backend.controller;

import com.example.backend.model.dto.ConfirmMedicationResponseDTO;
import com.example.backend.model.dto.TodayPlanResponseDTO;
import com.example.backend.model.dto.WeeklyMedicationResponseDTO;
import com.example.backend.model.dto.ReminderResponseDTO;
import com.example.backend.model.dto.AddToPlanRequest;
import com.example.backend.model.dto.MedicationActionRequest;
import com.example.backend.service.PlanService;
import com.example.backend.service.ProgressiveReminderService;
import com.example.backend.common.ResponseResult;
import com.example.backend.task.ScheduledTask;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final ScheduledTask scheduledTask;
    private final ProgressiveReminderService progressiveReminderService;

    /**
     * 获取当前认证用户的ID（数据库主键）
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }
        return (Long) authentication.getPrincipal();
    }

    /**
     * 7.1 获取今日用药计划
     */
    @GetMapping("/today")
    public ResponseResult<TodayPlanResponseDTO> getTodayPlan() {
        return ResponseResult.success(planService.getTodayPlan());
    }

    /**
     * 7.2 确认服药
     */
    @PutMapping("/{planId}/confirm")
    public ResponseResult<ConfirmMedicationResponseDTO> confirmMedication(@PathVariable Long planId) {
        return ResponseResult.success("已记录服用", planService.confirmMedication(planId));
    }

    /**
     * 7.3 跳过服药
     */
    @PutMapping("/{planId}/skip")
    public ResponseResult<Void> skipMedication(@PathVariable Long planId) {
        planService.skipMedication(planId);
        return ResponseResult.success("已跳过", null);
    }

    /**
     * 7.4 获取待提醒记录（轮询用）
     */
    @GetMapping("/reminders")
    public ResponseResult<List<ReminderResponseDTO>> getPendingReminders() {
        return ResponseResult.success(planService.getPendingReminders());
    }

    /**
     * 7.5 根据家庭药箱自动生成今日用药计划
     */
    @GetMapping("/generate-today")
    public ResponseResult<TodayPlanResponseDTO> generateDailyPlanFromMedicineBox() {
        Long userId = getCurrentUserId();
        return ResponseResult.success(planService.generateDailyPlanFromMedicineBox(userId));
    }

    /**
     * 7.6 将药箱中的药品添加到用药计划
     */
    @PostMapping("/add-from-box")
    public ResponseResult<Void> addBoxItemToPlan(@RequestBody AddToPlanRequest request) {
        Long userId = getCurrentUserId();
        planService.addBoxItemToMedicationPlan(userId, request.getBoxItemId(), request.getTimeSlots());
        return ResponseResult.success("已添加到用药日历", null);
    }

    /**
     * 7.7 清空用户的所有用药计划（仅用于测试）
     */
    @DeleteMapping("/clear-all")
    public ResponseResult<Void> clearAllPlans() {
        Long userId = getCurrentUserId();
        planService.clearAllPlans(userId);
        return ResponseResult.success("已清空所有用药计划", null);
    }

    /**
     * 7.8 获取一周内的用药记录（包括已删除但在查询范围内的记录）
     */
    @GetMapping("/weekly")
    public ResponseResult<WeeklyMedicationResponseDTO> getWeeklyMedicationRecords() {
        Long userId = getCurrentUserId();
        return ResponseResult.success(planService.getWeeklyMedicationRecords(userId));
    }

    /**
     * 7.9 统一用药操作接口（幂等）
     * 支持 confirm（确认服药，扣减库存）、skip（跳过服药，不扣减库存）、undo（撤销服药，恢复库存）
     */
    @PutMapping("/{planId}/action")
    public ResponseResult<ConfirmMedicationResponseDTO> executeMedicationAction(
            @PathVariable Long planId,
            @RequestBody MedicationActionRequest request) {
        Long userId = getCurrentUserId();
        if (request.getAction() == null || request.getAction().isEmpty()) {
            return ResponseResult.fail("参数错误：action 不能为空");
        }
        return ResponseResult.success(planService.executeMedicationAction(planId, userId, request.getAction()));
    }

    /**
     * 7.10 手动触发生成下一天用药计划（仅用于测试）
     * 注意：此接口会立即执行定时任务逻辑，生产环境应删除或禁用
     */
    @PostMapping("/test/generate-next-day")
    public ResponseResult<Void> testGenerateNextDayPlan() {
        scheduledTask.generateNextDayMedicationPlan();
        return ResponseResult.success("已手动触发生成下一天用药计划", null);
    }

    /**
     * 7.11 手动触发渐进式提醒扫描（仅用于测试）
     */
    @PostMapping("/test/progressive-reminder")
    public ResponseResult<Void> testProgressiveReminder() {
        progressiveReminderService.processProgressiveReminders();
        return ResponseResult.success("已手动触发渐进式提醒扫描", null);
    }
}
