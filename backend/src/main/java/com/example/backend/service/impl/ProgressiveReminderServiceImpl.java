package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.GuardianElderRelationMapper;
import com.example.backend.mapper.MedicationPlanMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.GuardianElderRelation;
import com.example.backend.model.entity.MedicationPlan;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.ElderNotificationService;
import com.example.backend.service.ProgressiveReminderService;
import com.example.backend.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 渐进式提醒服务实现
 * 阶段推进：none → pre_remind(提前15min) → due_now(到时) → notify_family(超时10min通知家属)
 * 说明：原 overdue 阶段已合并到 notify_family，避免到时提醒关闭后1分钟又反复弹窗。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressiveReminderServiceImpl implements ProgressiveReminderService {

    private final MedicationPlanMapper medicationPlanMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final ElderNotificationService elderNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final GuardianElderRelationMapper guardianElderRelationMapper;
    private final UserMapper userMapper;

    /** 超时阈值：超过用药时间该分钟数后通知家属 */
    private static final int OVERDUE_FAMILY_THRESHOLD = 10;

    @Override
    public void processProgressiveReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 查询今日所有 pending 状态的用药计划
        LambdaQueryWrapper<MedicationPlan> query = new LambdaQueryWrapper<>();
        query.eq(MedicationPlan::getPlanDate, today)
                .eq(MedicationPlan::getStatus, "pending");
        List<MedicationPlan> pendingPlans = medicationPlanMapper.selectList(query);

        int preRemindCount = 0;
        int dueNowCount = 0;
        int familyNotifyCount = 0;

        for (MedicationPlan plan : pendingPlans) {
            LocalTime slotTime = getTimeSlotTime(plan.getTimeSlot());
            int remindBefore = plan.getRemindBefore() != null ? plan.getRemindBefore() : 15;
            String currentStage = plan.getReminderStage();

            // 计算提前提醒时间点
            LocalTime preRemindTime = slotTime.minusMinutes(remindBefore);
            // 超时通知家属时间点
            LocalTime familyNotifyTime = slotTime.plusMinutes(OVERDUE_FAMILY_THRESHOLD);

            String newStage = currentStage;

            if (currentStage == null || "none".equals(currentStage)) {
                // 阶段1：提前提醒
                if (!now.isBefore(preRemindTime)) {
                    newStage = "pre_remind";
                    preRemindCount++;
                }
            } else if ("pre_remind".equals(currentStage)) {
                // 阶段2：到时提醒
                if (!now.isBefore(slotTime)) {
                    newStage = "due_now";
                    dueNowCount++;
                }
            } else if ("due_now".equals(currentStage) || "overdue".equals(currentStage)) {
                // 阶段3：超时10分钟，通知家属
                // 说明：原 overdue 阶段已合并，到时提醒后直接在超时10分钟时通知家属，
                // 避免用户关闭到时提醒后1分钟又被反复打扰；overdue 分支兼容历史数据。
                if (!now.isBefore(familyNotifyTime)) {
                    newStage = "notify_family";
                    familyNotifyCount++;
                }
            }

            // 阶段有变化时更新
            if (newStage != null && !newStage.equals(currentStage)) {
                updatePlanStage(plan, newStage);

                // 根据新阶段执行对应操作
                switch (newStage) {
                    case "pre_remind":
                        sendPreRemindNotification(plan);
                        break;
                    case "due_now":
                        sendDueNowNotification(plan);
                        break;
                    case "notify_family":
                        notifyFamily(plan);
                        break;
                }
            }
        }

        if (preRemindCount + dueNowCount + familyNotifyCount > 0) {
            log.info("渐进式提醒处理完成 - 提前提醒: {}, 到时: {}, 通知家属: {}",
                    preRemindCount, dueNowCount, familyNotifyCount);
        }
    }

    /**
     * 更新计划提醒阶段
     */
    private void updatePlanStage(MedicationPlan plan, String stage) {
        LambdaUpdateWrapper<MedicationPlan> update = new LambdaUpdateWrapper<>();
        update.eq(MedicationPlan::getId, plan.getId())
                .set(MedicationPlan::getReminderStage, stage);
        medicationPlanMapper.update(null, update);
        plan.setReminderStage(stage);
    }

    /**
     * 阶段1：提前提醒 - 通过WebSocket推送温和提醒
     */
    private void sendPreRemindNotification(MedicationPlan plan) {
        String drugName = getDrugName(plan);
        String timeSlotLabel = getTimeSlotLabel(plan.getTimeSlot());
        String title = "即将到服药时间";
        String content = String.format("您%s的%s将在15分钟后需要服用，请做好准备。", timeSlotLabel, drugName);

        try {
            elderNotificationService.createNotification(
                    plan.getUserId(), "medication_reminder", title, content, null);
        } catch (Exception e) {
            log.warn("提前提醒推送失败 - planId: {}", plan.getId(), e);
        }
    }

    /**
     * 阶段2：到时提醒 - 通过WebSocket推送正常提醒
     */
    private void sendDueNowNotification(MedicationPlan plan) {
        String drugName = getDrugName(plan);
        String timeSlotLabel = getTimeSlotLabel(plan.getTimeSlot());
        String title = "该服药了";
        String content = String.format("您%s的%s已到服药时间，请及时服用。", timeSlotLabel, drugName);

        try {
            elderNotificationService.createNotification(
                    plan.getUserId(), "medication_reminder", title, content, null);
        } catch (Exception e) {
            log.warn("到时提醒推送失败 - planId: {}", plan.getId(), e);
        }
    }

    /**
     * 阶段3：通知家属 - 短信通知 + 标记漏服 + 老人端通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void notifyFamily(MedicationPlan plan) {
        // 标记为漏服
        plan.setStatus("missed");
        medicationPlanMapper.updateById(plan);

        String drugName = getDrugName(plan);
        String timeSlotLabel = getTimeSlotLabel(plan.getTimeSlot());

        // 同时给老人端推送通知（让老人知道已通知家属）
        try {
            String elderTitle = "已通知家属";
            String elderContent = String.format("您%s的%s已超时较久未服用，已通知您的家属关注，请尽快服药！",
                    timeSlotLabel, drugName);
            elderNotificationService.createNotification(
                    plan.getUserId(), "medication_reminder", elderTitle, elderContent, null);
        } catch (Exception e) {
            log.warn("老人端漏服通知推送失败 - planId: {}", plan.getId(), e);
        }

        // 查询老人的监护人
        LambdaQueryWrapper<GuardianElderRelation> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.eq(GuardianElderRelation::getElderId, plan.getUserId())
                .eq(GuardianElderRelation::getStatus, "active");
        List<GuardianElderRelation> relations = guardianElderRelationMapper.selectList(relationQuery);

        // 查询老人姓名
        SysUser elder = userMapper.selectById(plan.getUserId());
        String elderName = elder != null ? elder.getRealName() : "老人";

        String message = String.format("【用药超时提醒】%s今日%s的%s已超时10分钟未服用，请及时关注！",
                elderName, timeSlotLabel, drugName);

        for (GuardianElderRelation relation : relations) {
            SysUser guardian = userMapper.selectById(relation.getGuardianId());
            String phone = guardian != null ? guardian.getPhone() : "";

            try {
                smsNotificationService.sendNotification(
                        relation.getGuardianId(), plan.getUserId(),
                        "missed_dose", message, phone);
            } catch (Exception e) {
                log.error("通知家属失败 - guardianId: {}, elderId: {}",
                        relation.getGuardianId(), plan.getUserId(), e);
            }
        }

        log.info("已通知家属漏服情况 - planId: {}, elderId: {}", plan.getId(), plan.getUserId());
    }

    /**
     * 时段对应时间点
     */
    private LocalTime getTimeSlotTime(String timeSlot) {
        return switch (timeSlot) {
            case "morning" -> LocalTime.of(8, 0);
            case "noon" -> LocalTime.of(12, 0);
            case "evening" -> LocalTime.of(18, 0);
            case "before_bed" -> LocalTime.of(21, 0);
            default -> LocalTime.of(8, 0);
        };
    }

    /**
     * 时段英文标识转中文
     */
    private String getTimeSlotLabel(String timeSlot) {
        return switch (timeSlot) {
            case "morning" -> "早上";
            case "noon" -> "中午";
            case "evening" -> "晚上";
            case "before_bed" -> "睡前";
            default -> timeSlot;
        };
    }

    /**
     * 获取药品名称
     */
    private String getDrugName(MedicationPlan plan) {
        if (plan.getDrugId() != null) {
            DrugBase drug = drugBaseMapper.selectById(plan.getDrugId());
            if (drug != null) {
                return drug.getCommonName() != null ? drug.getCommonName() : "药物";
            }
        }
        return "药物";
    }
}
