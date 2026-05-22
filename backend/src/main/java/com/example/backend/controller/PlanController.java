package com.example.backend.controller;

import com.example.backend.model.dto.ConfirmMedicationResponseDTO;
import com.example.backend.model.dto.TodayPlanResponseDTO;
import com.example.backend.model.dto.ReminderResponseDTO;
import com.example.backend.model.dto.AddToPlanRequest;
import com.example.backend.service.PlanService;
import com.example.backend.common.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    // 原有接口保留不变

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
    public ResponseResult<TodayPlanResponseDTO> generateDailyPlanFromMedicineBox(@RequestParam Long userId) {
        return ResponseResult.success(planService.generateDailyPlanFromMedicineBox(userId));
    }

    /**
     * 7.6 将药箱中的药品添加到用药计划
     */
    @PostMapping("/add-from-box")
    public ResponseResult<Void> addBoxItemToPlan(@RequestBody AddToPlanRequest request) {
        planService.addBoxItemToMedicationPlan(request.getUserId(), request.getBoxItemId(), request.getTimeSlots());
        return ResponseResult.success("已添加到用药日历", null);
    }
}
