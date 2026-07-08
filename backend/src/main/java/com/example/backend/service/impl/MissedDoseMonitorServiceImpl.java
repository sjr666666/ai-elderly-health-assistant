package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.GuardianElderRelationMapper;
import com.example.backend.mapper.MedicationPlanMapper;
import com.example.backend.mapper.SmsNotificationLogMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.entity.*;
import com.example.backend.model.enums.RelationStatus;
import com.example.backend.service.MissedDoseMonitorService;
import com.example.backend.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 漏服监控服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissedDoseMonitorServiceImpl implements MissedDoseMonitorService {

    private final GuardianElderRelationMapper guardianElderRelationMapper;
    private final MedicationPlanMapper medicationPlanMapper;
    private final SmsNotificationLogMapper smsNotificationLogMapper;
    private final UserMapper userMapper;
    private final SmsNotificationService smsNotificationService;

    @Override
    public void checkMissedDoses() {
        log.info("开始检查漏服情况");

        LocalDate today = LocalDate.now();

        // 查询所有今日的待服用计划
        LambdaQueryWrapper<MedicationPlan> planQuery = new LambdaQueryWrapper<>();
        planQuery.eq(MedicationPlan::getPlanDate, today)
                .eq(MedicationPlan::getStatus, MedicationPlan.Status.PENDING.getCode());
        List<MedicationPlan> pendingPlans = medicationPlanMapper.selectList(planQuery);

        int missedCount = 0;
        for (MedicationPlan plan : pendingPlans) {
            // 判断是否超时：根据时段判断是否已过时
            if (isOverdue(plan)) {
                // 标记为漏服
                plan.setStatus(MedicationPlan.Status.MISSED.getCode());
                medicationPlanMapper.updateById(plan);

                // 通知家属
                notifyGuardians(plan);
                missedCount++;
            }
        }

        log.info("漏服检查完成，共发现 {} 条漏服记录", missedCount);
    }

    /**
     * 判断用药计划是否超时
     */
    private boolean isOverdue(MedicationPlan plan) {
        LocalTime now = LocalTime.now();
        String timeSlot = plan.getTimeSlot();

        // 根据时段判断是否已过时
        switch (timeSlot) {
            case "morning":
                return now.isAfter(LocalTime.of(10, 0));
            case "noon":
                return now.isAfter(LocalTime.of(14, 0));
            case "evening":
                return now.isAfter(LocalTime.of(21, 0));
            case "before_bed":
                return now.isAfter(LocalTime.of(23, 0));
            default:
                return now.isAfter(LocalTime.of(21, 0));
        }
    }

    /**
     * 时段英文标识转中文
     */
    private String getTimeSlotLabel(String timeSlot) {
        switch (timeSlot) {
            case "morning": return "早晨";
            case "noon": return "中午";
            case "evening": return "傍晚";
            case "before_bed": return "睡前";
            default: return timeSlot;
        }
    }

    /**
     * 通知家属漏服情况
     */
    private void notifyGuardians(MedicationPlan plan) {
        // 查询老人的监护人
        LambdaQueryWrapper<GuardianElderRelation> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.eq(GuardianElderRelation::getElderId, plan.getUserId())
                .eq(GuardianElderRelation::getStatus, RelationStatus.ACTIVE.getCode());
        List<GuardianElderRelation> relations = guardianElderRelationMapper.selectList(relationQuery);

        // 查询老人姓名
        SysUser elder = userMapper.selectById(plan.getUserId());
        String elderName = elder != null ? elder.getRealName() : "老人";

        String message = String.format("【用药提醒】%s今日%s时段的药物未按时服用，请关注！", elderName, getTimeSlotLabel(plan.getTimeSlot()));

        for (GuardianElderRelation relation : relations) {
            // 查询监护人手机号
            SysUser guardian = userMapper.selectById(relation.getGuardianId());
            String phone = guardian != null ? guardian.getPhone() : "";

            try {
                smsNotificationService.sendNotification(
                        relation.getGuardianId(),
                        plan.getUserId(),
                        "missed_dose",
                        message,
                        phone
                );
            } catch (Exception e) {
                log.error("通知家属失败 - guardianId: {}, elderId: {}",
                        relation.getGuardianId(), plan.getUserId(), e);
            }
        }
    }
}
