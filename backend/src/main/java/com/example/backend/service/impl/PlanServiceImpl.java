package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.model.entity.*;
import com.example.backend.mapper.*;
import com.example.backend.model.dto.*;
import com.example.backend.service.PlanService;
import com.example.backend.common.BusinessException;
import com.example.backend.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl extends ServiceImpl<MedicationPlanMapper, MedicationPlan> implements PlanService {

    private static final Logger logger = LoggerFactory.getLogger(PlanServiceImpl.class);

    private final MedicationPlanMapper medicationPlanMapper;
    private final MedicationLogMapper medicationLogMapper;
    private final ReminderLogMapper reminderLogMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final UserMedicineBoxMapper userMedicineBoxMapper;
    private final UserMapper userMapper;

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SysUser) {
            return ((SysUser) principal).getId();
        }
        throw new BusinessException(ResponseCode.UNAUTHORIZED, "用户未登录");
    }

    private String getTimeSlotLabel(String timeSlot) {
        return switch (timeSlot) {
            case "morning" -> "早上";
            case "afternoon" -> "下午";
            case "evening" -> "晚上";
            case "night" -> "睡前";
            default -> "其他";
        };
    }

    private MedicationPlan validatePlanOwnership(Long planId, Long userId) {
        // @TableLogic自动过滤deleted=1的数据，无需手动添加
        MedicationPlan plan = getOne(new LambdaQueryWrapper<MedicationPlan>()
                .eq(MedicationPlan::getId, planId)
                .eq(MedicationPlan::getUserId, userId));
        if (plan == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用药计划不存在");
        }
        return plan;
    }

    private void checkDuplicateOperation(Long planId) {
        // @TableLogic自动过滤deleted=1的数据
        Long count = medicationLogMapper.selectCount(new LambdaQueryWrapper<MedicationLog>()
                .eq(MedicationLog::getPlanId, planId));
        if (count > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "今日该时段已记录过");
        }
    }

    private void markReminderAsRead(Long userId, Long planId) {
        // @TableLogic自动过滤deleted=1的数据
        reminderLogMapper.update(null, new LambdaUpdateWrapper<ReminderLog>()
                .eq(ReminderLog::getUserId, userId)
                .eq(ReminderLog::getPlanId, planId)
                .eq(ReminderLog::getRemindType, "missed_alert")
                .eq(ReminderLog::getStatus, "sent")
                .set(ReminderLog::getStatus, "read")
                .set(ReminderLog::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public TodayPlanResponseDTO getTodayPlan() {
        Long userId = getCurrentUserId();
        LocalDate today = LocalDate.now();

        // Mapper中的SQL已经包含deleted=0条件，保持不变
        List<MedicationPlan> plans = medicationPlanMapper.selectUserDailyPlans(userId, today);
        if (plans.isEmpty()) {
            TodayPlanResponseDTO response = new TodayPlanResponseDTO();
            response.setDate(today);
            response.setItems(List.of());
            return response;
        }

        List<MedicationLog> logs = medicationLogMapper.selectUserDailyLogs(userId, today);
        Map<Long, String> planStatusMap = logs.stream()
                .collect(Collectors.toMap(MedicationLog::getPlanId, MedicationLog::getStatus));

        List<Long> drugIds = plans.stream().map(MedicationPlan::getDrugId).collect(Collectors.toList());
        Map<Long, DrugBase> drugMap = drugBaseMapper.selectBatchIds(drugIds).stream()
                .collect(Collectors.toMap(DrugBase::getId, drug -> drug));

        List<TodayPlanItemDTO> items = plans.stream().map(plan -> {
            TodayPlanItemDTO item = new TodayPlanItemDTO();
            item.setPlanId(plan.getId());

            // ==================== 只改这一行！用你DrugBase实际的getter ====================
            DrugBase drug = drugMap.get(plan.getDrugId());
            item.setDrugName(drug != null ? drug.getCommonName() : "未知药品");
            // 如果是name就用drug.getName()，是medicineName就用drug.getMedicineName()
            // ==========================================================================

            item.setDosageAtTime(plan.getDosageAtTime());
            item.setTimeSlot(plan.getTimeSlot());
            item.setTimeSlotLabel(getTimeSlotLabel(plan.getTimeSlot()));
            item.setRemindBefore(plan.getRemindBefore());
            item.setStatus(planStatusMap.getOrDefault(plan.getId(), "pending"));
            return item;
        }).collect(Collectors.toList());

        TodayPlanResponseDTO response = new TodayPlanResponseDTO();
        response.setDate(today);
        response.setItems(items);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfirmMedicationResponseDTO confirmMedication(Long planId) {
        Long userId = getCurrentUserId();
        MedicationPlan plan = validatePlanOwnership(planId, userId);
        checkDuplicateOperation(planId);

        MedicationLog log = new MedicationLog();
        log.setPlanId(planId);
        log.setUserId(userId);
        log.setStatus("taken");
        log.setConfirmedAt(LocalDateTime.now());
        // created_at和updated_at由MyMetaObjectHandler自动填充，无需手动设置
        medicationLogMapper.insert(log);

        plan.setStatus("completed");
        // updated_at由MyMetaObjectHandler自动填充
        updateById(plan);

        markReminderAsRead(userId, planId);

        ConfirmMedicationResponseDTO response = new ConfirmMedicationResponseDTO();
        response.setLogId(log.getId());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void skipMedication(Long planId) {
        Long userId = getCurrentUserId();
        MedicationPlan plan = validatePlanOwnership(planId, userId);
        checkDuplicateOperation(planId);

        MedicationLog log = new MedicationLog();
        log.setPlanId(planId);
        log.setUserId(userId);
        log.setStatus("skipped");
        log.setConfirmedAt(LocalDateTime.now());
        // created_at和updated_at自动填充
        medicationLogMapper.insert(log);

        plan.setStatus("cancelled");
        // updated_at自动填充
        updateById(plan);

        markReminderAsRead(userId, planId);
    }

    @Override
    public List<ReminderResponseDTO> getPendingReminders() {
        Long userId = getCurrentUserId();
        List<ReminderLog> reminders = reminderLogMapper.selectUnreadReminders(userId);

        return reminders.stream().map(reminder -> {
            ReminderResponseDTO dto = new ReminderResponseDTO();
            dto.setReminderId(reminder.getId());
            dto.setType(reminder.getRemindType());
            dto.setContent(reminder.getContent());
            dto.setCreatedAt(reminder.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public TodayPlanResponseDTO generateDailyPlanFromMedicineBox(Long userId) {
        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(SysUser::getUserId, userId);
        SysUser user = userMapper.selectOne(userQueryWrapper);
        
        if (user == null) {
            TodayPlanResponseDTO emptyResponse = new TodayPlanResponseDTO();
            emptyResponse.setDate(LocalDate.now());
            emptyResponse.setItems(List.of());
            return emptyResponse;
        }
        
        Long actualUserId = user.getId();
        LocalDate today = LocalDate.now();

        // 1. 查询数据库中已有的今日用药计划
        List<MedicationPlan> existingPlans = medicationPlanMapper.selectUserDailyPlans(actualUserId, today);
        
        // 按药品ID分组已有的计划（用于判断哪些药品已有用户选择的时间段）
        Map<Long, List<MedicationPlan>> existingPlansByDrug = existingPlans.stream()
                .collect(Collectors.groupingBy(MedicationPlan::getDrugId));
        
        Map<String, MedicationPlan> existingPlanMap = new HashMap<>();
        for (MedicationPlan plan : existingPlans) {
            String key = plan.getDrugId() + "_" + plan.getTimeSlot();
            existingPlanMap.put(key, plan);
        }

        // 2. 查询家庭药箱中当前用户需要服用的药品（状态为active，且在服用日期范围内）
        LambdaQueryWrapper<UserMedicineBox> boxQuery = new LambdaQueryWrapper<>();
        boxQuery.eq(UserMedicineBox::getUserId, actualUserId)
                .eq(UserMedicineBox::getStatus, "active")
                .le(UserMedicineBox::getStartDate, today)
                .and(w -> w.isNull(UserMedicineBox::getEndDate).or().ge(UserMedicineBox::getEndDate, today));
        List<UserMedicineBox> medicineBoxItems = userMedicineBoxMapper.selectList(boxQuery);

        if (medicineBoxItems.isEmpty()) {
            TodayPlanResponseDTO emptyResponse = new TodayPlanResponseDTO();
            emptyResponse.setDate(today);
            emptyResponse.setItems(List.of());
            return emptyResponse;
        }

        // 3. 获取所有药品ID，查询药品基础信息
        List<Long> drugIds = medicineBoxItems.stream()
                .map(UserMedicineBox::getDrugId)
                .collect(Collectors.toList());
        Map<Long, DrugBase> drugMap = drugBaseMapper.selectBatchIds(drugIds).stream()
                .collect(Collectors.toMap(DrugBase::getId, drug -> drug));

        // 4. 获取今日服药记录
        List<MedicationLog> logs = medicationLogMapper.selectUserDailyLogs(actualUserId, today);
        Map<Long, String> planStatusMap = logs.stream()
                .collect(Collectors.toMap(MedicationLog::getPlanId, MedicationLog::getStatus));

        // 5. 根据药箱中的药品生成用药计划 - 只包含用户已选择过时间段的药品
        List<TodayPlanItemDTO> items = new ArrayList<>();

        for (UserMedicineBox boxItem : medicineBoxItems) {
            DrugBase drug = drugMap.get(boxItem.getDrugId());
            if (drug == null) continue;

            // 关键修改：只有用户已经选择过时间段的药品才生成计划
            if (!existingPlansByDrug.containsKey(boxItem.getDrugId())) {
                continue; // 跳过未选择时间段的药品
            }

            String drugName = drug.getCommonName();
            String dosage = boxItem.getDosage();

            // 使用用户已选择的时间段
            List<String> timeSlots = existingPlansByDrug.get(boxItem.getDrugId()).stream()
                    .map(MedicationPlan::getTimeSlot)
                    .distinct()
                    .collect(Collectors.toList());

            for (String timeSlot : timeSlots) {
                String key = boxItem.getDrugId() + "_" + timeSlot;

                TodayPlanItemDTO item = new TodayPlanItemDTO();

                // 如果已有计划，使用已有的planId和状态
                if (existingPlanMap.containsKey(key)) {
                    MedicationPlan existingPlan = existingPlanMap.get(key);
                    item.setPlanId(existingPlan.getId());
                    item.setStatus(planStatusMap.getOrDefault(existingPlan.getId(), "pending"));
                } else {
                    item.setPlanId(null); // 新生成的计划，暂无ID
                    item.setStatus("pending");
                }

                item.setDrugId(boxItem.getDrugId());
                item.setDrugName(drugName);
                item.setDosageAtTime(dosage);
                item.setTimeSlot(timeSlot);
                item.setTimeSlotLabel(getTimeSlotLabel(timeSlot));
                item.setRemindBefore(15); // 默认提前15分钟提醒
                
                // 设置药箱条目ID和剩余数量（用于更新库存）
                item.setBoxItemId(boxItem.getId());
                item.setRemainingQuantity(boxItem.getRemainingQuantity() != null ? boxItem.getRemainingQuantity() : boxItem.getTotalQuantity());

                items.add(item);
            }
        }

        // 按时间段排序：早上 -> 中午 -> 晚上 -> 睡前
        items.sort((a, b) -> {
            int orderA = getTimeSlotOrder(a.getTimeSlot());
            int orderB = getTimeSlotOrder(b.getTimeSlot());
            return Integer.compare(orderA, orderB);
        });

        TodayPlanResponseDTO response = new TodayPlanResponseDTO();
        response.setDate(today);
        response.setItems(items);
        return response;
    }

    /**
     * 将频率字符串解析为服药时间段列表
     */
    private List<String> parseFrequencyToTimeSlots(String frequency) {
        List<String> timeSlots = new ArrayList<>();
        if (frequency == null || frequency.isEmpty()) {
            timeSlots.add("morning"); // 默认早上一次
            return timeSlots;
        }

        String freq = frequency.toLowerCase();

        if (freq.contains("四次") || freq.contains("4次")) {
            timeSlots.addAll(Arrays.asList("morning", "noon", "evening", "before_bed"));
        } else if (freq.contains("三次") || freq.contains("3次")) {
            timeSlots.addAll(Arrays.asList("morning", "noon", "evening"));
        } else if (freq.contains("两次") || freq.contains("2次") || freq.contains("一日二")) {
            timeSlots.addAll(Arrays.asList("morning", "evening"));
        } else if (freq.contains("一次") || freq.contains("1次") || freq.contains("每日") || freq.contains("qd")) {
            // 判断具体时间
            if (freq.contains("睡前") || freq.contains("晚上") || freq.contains("qn")) {
                timeSlots.add("before_bed");
            } else if (freq.contains("中午") || freq.contains("下午")) {
                timeSlots.add("noon");
            } else if (freq.contains("晚上")) {
                timeSlots.add("evening");
            } else {
                timeSlots.add("morning"); // 默认早上
            }
        } else {
            // 无法识别的频率，默认早上一次
            timeSlots.add("morning");
        }

        return timeSlots;
    }

    /**
     * 获取时间段的排序权重
     */
    private int getTimeSlotOrder(String timeSlot) {
        return switch (timeSlot) {
            case "morning" -> 1;
            case "noon" -> 2;
            case "evening" -> 3;
            case "before_bed" -> 4;
            default -> 5;
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBoxItemToMedicationPlan(Long userId, Long boxItemId, List<String> timeSlots) {
        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(SysUser::getUserId, userId);
        SysUser user = userMapper.selectOne(userQueryWrapper);
        
        if (user == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户不存在");
        }
        
        Long actualUserId = user.getId();
        LocalDate today = LocalDate.now();

        // 1. 查询药箱条目
        UserMedicineBox boxItem = userMedicineBoxMapper.selectById(boxItemId);
        if (boxItem == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "药箱条目不存在");
        }

        // 2. 验证该条目属于当前用户
        if (!boxItem.getUserId().equals(actualUserId)) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "无权操作该药品");
        }

        // 3. 验证该药品是否在有效期内
        if (boxItem.getStartDate() != null && boxItem.getStartDate().isAfter(today)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该药品尚未到开始服用日期");
        }
        if (boxItem.getEndDate() != null && boxItem.getEndDate().isBefore(today)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该药品已过服用结束日期");
        }

        // 4. 为每个选中的时间段创建用药计划
        for (String timeSlot : timeSlots) {
            // 检查今天是否已有该药品在该时间段的计划
            LambdaQueryWrapper<MedicationPlan> existingQuery = new LambdaQueryWrapper<>();
            existingQuery.eq(MedicationPlan::getUserId, actualUserId)
                    .eq(MedicationPlan::getDrugId, boxItem.getDrugId())
                    .eq(MedicationPlan::getPlanDate, today)
                    .eq(MedicationPlan::getTimeSlot, timeSlot);
            
            Long existingCount = medicationPlanMapper.selectCount(existingQuery);
            if (existingCount > 0) {
                logger.info("该药品在{}时段已有用药计划，跳过", getTimeSlotLabel(timeSlot));
                continue; // 已有计划则跳过
            }

            // 创建新的用药计划
            MedicationPlan plan = new MedicationPlan();
            plan.setUserId(actualUserId);
            plan.setDrugId(boxItem.getDrugId());
            plan.setBoxItemId(boxItemId);
            plan.setPlanDate(today);
            plan.setTimeSlot(timeSlot);
            plan.setDosageAtTime(boxItem.getDosage()); // 使用药箱中的用量
            plan.setStatus("pending");
            plan.setRemindBefore(15); // 默认提前15分钟提醒

            medicationPlanMapper.insert(plan);
            logger.info("已添加用药计划 - 药品ID: {}, 时段: {}", boxItem.getDrugId(), timeSlot);
        }
    }

    @Override
    public void clearAllPlans(Long userId) {
        // 根据雪花算法 user_id 查询实际的自增主键 id
        LambdaQueryWrapper<SysUser> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(SysUser::getUserId, userId);
        SysUser user = userMapper.selectOne(userQueryWrapper);
        
        if (user == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户不存在");
        }
        
        Long actualUserId = user.getId();
        
        // 删除该用户的所有用药计划
        LambdaQueryWrapper<MedicationPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MedicationPlan::getUserId, actualUserId);
        
        int deletedCount = medicationPlanMapper.delete(queryWrapper);
        logger.info("已清空用户 {} 的所有用药计划，共删除 {} 条记录", userId, deletedCount);
    }
}