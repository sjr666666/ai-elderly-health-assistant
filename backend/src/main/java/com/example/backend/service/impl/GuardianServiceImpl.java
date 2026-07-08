package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.BusinessException;
import com.example.backend.mapper.*;
import com.example.backend.model.dto.ElderMedicationPlanDTO;
import com.example.backend.model.dto.ElderMedicationPlanItemDTO;
import com.example.backend.model.dto.ElderSummaryDTO;
import com.example.backend.model.dto.ExpiringDrugDTO;
import com.example.backend.model.dto.GuardianDashboardDTO;
import com.example.backend.model.dto.GuardianRelationRequest;
import com.example.backend.model.dto.GuardianSummaryDTO;
import com.example.backend.model.entity.*;
import com.example.backend.model.enums.EventType;
import com.example.backend.model.enums.RelationStatus;
import com.example.backend.service.ElderNotificationService;
import com.example.backend.service.GuardianService;
import com.example.backend.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监护人服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuardianServiceImpl implements GuardianService {

    private final GuardianElderRelationMapper guardianElderRelationMapper;
    private final UserMapper userMapper;
    private final MedicationPlanMapper medicationPlanMapper;
    private final EmergencyEventMapper emergencyEventMapper;
    private final UserMedicineBoxMapper userMedicineBoxMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final SmsNotificationService smsNotificationService;
    private final ElderNotificationService elderNotificationService;

    @Override
    public GuardianDashboardDTO getDashboard(Long guardianId) {
        log.info("获取监护人仪表盘数据 - guardianId: {}", guardianId);

        List<ElderSummaryDTO> elderList = getElderList(guardianId);

        // 统计活跃告警数（所有关联老人的待处理事件总数）
        int activeAlertCount = 0;
        for (ElderSummaryDTO elder : elderList) {
            activeAlertCount += elder.getActiveAlertCount();
        }

        return GuardianDashboardDTO.builder()
                .elderCount(elderList.size())
                .activeAlertCount(activeAlertCount)
                .unreadNotificationCount(smsNotificationService.getUnreadCount(guardianId))
                .elders(elderList)
                .build();
    }

    @Override
    public List<ElderSummaryDTO> getElderList(Long guardianId) {
        log.info("获取关联老人列表 - guardianId: {}", guardianId);

        // 1. 查询关联关系
        LambdaQueryWrapper<GuardianElderRelation> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.eq(GuardianElderRelation::getGuardianId, guardianId)
                .eq(GuardianElderRelation::getStatus, RelationStatus.ACTIVE.getCode());
        List<GuardianElderRelation> relations = guardianElderRelationMapper.selectList(relationQuery);

        if (relations.isEmpty()) {
            log.info("未找到关联老人 - guardianId: {}", guardianId);
            return new ArrayList<>();
        }

        List<Long> elderIds = relations.stream()
                .map(GuardianElderRelation::getElderId)
                .collect(Collectors.toList());

        // 2. 批量查询老人基本信息
        List<SysUser> elders = userMapper.selectBatchIds(elderIds);
        Map<Long, SysUser> elderMap = elders.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 3. 批量查询今日用药计划
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<MedicationPlan> planQuery = new LambdaQueryWrapper<>();
        planQuery.in(MedicationPlan::getUserId, elderIds)
                .eq(MedicationPlan::getPlanDate, today);
        List<MedicationPlan> allPlans = medicationPlanMapper.selectList(planQuery);
        Map<Long, List<MedicationPlan>> plansByElder = allPlans.stream()
                .collect(Collectors.groupingBy(MedicationPlan::getUserId));

        // 4. 批量查询活跃告警数
        LambdaQueryWrapper<EmergencyEvent> eventQuery = new LambdaQueryWrapper<>();
        eventQuery.in(EmergencyEvent::getElderId, elderIds)
                .eq(EmergencyEvent::getIsResolved, 0);
        List<EmergencyEvent> activeEvents = emergencyEventMapper.selectList(eventQuery);
        Map<Long, Long> alertCountByElder = activeEvents.stream()
                .collect(Collectors.groupingBy(EmergencyEvent::getElderId, Collectors.counting()));

        // 5. 批量查询用药数量
        LambdaQueryWrapper<UserMedicineBox> boxQuery = new LambdaQueryWrapper<>();
        boxQuery.in(UserMedicineBox::getUserId, elderIds)
                .eq(UserMedicineBox::getStatus, UserMedicineBox.Status.ACTIVE.getCode());
        List<UserMedicineBox> allBoxes = userMedicineBoxMapper.selectList(boxQuery);
        Map<Long, Long> medCountByElder = allBoxes.stream()
                .collect(Collectors.groupingBy(UserMedicineBox::getUserId, Collectors.counting()));

        // 6. 组装结果
        List<ElderSummaryDTO> elderList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long elderId : elderIds) {
            SysUser elder = elderMap.get(elderId);
            if (elder == null) {
                log.warn("未找到老人 - elderId: {}", elderId);
                continue;
            }

            // 计算今日用药统计
            List<MedicationPlan> plans = plansByElder.getOrDefault(elderId, new ArrayList<>());
            int pendingCount = 0, takenCount = 0, missedCount = 0;
            for (MedicationPlan plan : plans) {
                String planStatus = plan.getStatus();
                if (MedicationPlan.Status.PENDING.getCode().equals(planStatus)) {
                    pendingCount++;
                } else if (MedicationPlan.Status.TAKEN.getCode().equals(planStatus)) {
                    takenCount++;
                } else if (MedicationPlan.Status.MISSED.getCode().equals(planStatus)) {
                    missedCount++;
                }
            }

            // 计算最后活跃时间（优先使用 lastActiveTime，兜底使用 updatedAt）
            String lastActiveTime = formatLastActiveTime(elder.getLastActiveTime(), elder.getUpdatedAt(), now);

            elderList.add(ElderSummaryDTO.builder()
                    .elderId(elder.getId())
                    .realName(elder.getRealName())
                    .age(elder.getAge())
                    .gender(elder.getGender())
                    .medicationCount(medCountByElder.getOrDefault(elderId, 0L).intValue())
                    .todayPendingCount(pendingCount)
                    .todayTakenCount(takenCount)
                    .todayMissedCount(missedCount)
                    .activeAlertCount(alertCountByElder.getOrDefault(elderId, 0L).intValue())
                    .lastActiveTime(lastActiveTime)
                    .build());
        }

        log.info("找到 {} 个关联老人 - guardianId: {}", elderList.size(), guardianId);
        return elderList;
    }

    @Override
    public List<GuardianSummaryDTO> getGuardianList(Long elderId) {
        log.info("老人查询已绑定家属列表 - elderId: {}", elderId);

        // 1. 查询所有 active 状态的家属绑定关系
        LambdaQueryWrapper<GuardianElderRelation> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.eq(GuardianElderRelation::getElderId, elderId)
                .eq(GuardianElderRelation::getStatus, RelationStatus.ACTIVE.getCode());
        List<GuardianElderRelation> relations = guardianElderRelationMapper.selectList(relationQuery);

        if (relations.isEmpty()) {
            log.info("老人未绑定任何家属 - elderId: {}", elderId);
            return new ArrayList<>();
        }

        List<Long> guardianIds = relations.stream()
                .map(GuardianElderRelation::getGuardianId)
                .collect(Collectors.toList());

        // 2. 批量查询家属用户信息
        List<SysUser> guardians = userMapper.selectBatchIds(guardianIds);
        Map<Long, SysUser> guardianMap = guardians.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 3. 组装结果
        List<GuardianSummaryDTO> guardianList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (GuardianElderRelation relation : relations) {
            SysUser guardian = guardianMap.get(relation.getGuardianId());
            if (guardian == null) {
                log.warn("未找到家属 - guardianId: {}", relation.getGuardianId());
                continue;
            }

            String lastActiveTime = formatLastActiveTime(guardian.getLastActiveTime(), guardian.getUpdatedAt(), now);

            guardianList.add(GuardianSummaryDTO.builder()
                    .guardianId(guardian.getId())
                    .realName(guardian.getRealName())
                    .phone(guardian.getPhone())
                    .age(guardian.getAge())
                    .gender(guardian.getGender())
                    .relationType(relation.getRelationType())
                    .lastActiveTime(lastActiveTime)
                    .build());
        }

        log.info("找到 {} 个绑定家属 - elderId: {}", guardianList.size(), elderId);
        return guardianList;
    }

    @Override
    public ElderSummaryDTO getElderDetail(Long guardianId, Long elderId) {
        log.info("获取老人详细信息 - guardianId: {}, elderId: {}", guardianId, elderId);
        return buildElderSummary(elderId);
    }

    @Override
    public boolean hasPermission(Long guardianId, Long elderId) {
        LambdaQueryWrapper<GuardianElderRelation> query = new LambdaQueryWrapper<>();
        query.eq(GuardianElderRelation::getGuardianId, guardianId)
                .eq(GuardianElderRelation::getElderId, elderId)
                .eq(GuardianElderRelation::getStatus, RelationStatus.ACTIVE.getCode());
        return guardianElderRelationMapper.selectCount(query) > 0;
    }

    @Override
    public GuardianElderRelation bindRelation(Long guardianId, GuardianRelationRequest request) {
        log.info("绑定监护关系 - guardianId: {}, elderId: {}, elderUsername: {}",
                guardianId, request.getElderId(), request.getElderUsername());

        Long elderId = request.getElderId();

        // 如果提供了elderUsername，先查找老人ID
        if (elderId == null && request.getElderUsername() != null) {
            LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
            userQuery.eq(SysUser::getUsername, request.getElderUsername())
                    .eq(SysUser::getRole, "elder");
            SysUser elder = userMapper.selectOne(userQuery);
            if (elder == null) {
                throw new BusinessException("未找到该老人用户：" + request.getElderUsername());
            }
            elderId = elder.getId();
        }

        if (elderId == null) {
            throw new BusinessException("请提供elderId或elderUsername");
        }

        // 验证elderId对应的用户角色是否为"elder"
        SysUser elderUser = userMapper.selectById(elderId);
        if (elderUser == null) {
            throw new RuntimeException("未找到该用户：elderId=" + elderId);
        }
        if (!"elder".equals(elderUser.getRole())) {
            throw new RuntimeException("只能绑定老人角色的用户，该用户角色为：" + elderUser.getRole());
        }

        // 检查是否已存在active关联
        LambdaQueryWrapper<GuardianElderRelation> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(GuardianElderRelation::getGuardianId, guardianId)
                .eq(GuardianElderRelation::getElderId, elderId)
                .eq(GuardianElderRelation::getStatus, RelationStatus.ACTIVE.getCode());
        if (guardianElderRelationMapper.selectCount(existQuery) > 0) {
            throw new BusinessException("监护关系已存在");
        }

        // 检查是否存在inactive关联（解绑过的），如果有则恢复
        LambdaQueryWrapper<GuardianElderRelation> inactiveQuery = new LambdaQueryWrapper<>();
        inactiveQuery.eq(GuardianElderRelation::getGuardianId, guardianId)
                .eq(GuardianElderRelation::getElderId, elderId)
                .eq(GuardianElderRelation::getStatus, RelationStatus.INACTIVE.getCode());
        GuardianElderRelation inactiveRelation = guardianElderRelationMapper.selectOne(inactiveQuery);
        if (inactiveRelation != null) {
            inactiveRelation.setStatus(RelationStatus.ACTIVE.getCode());
            inactiveRelation.setRelationType(request.getRelationType());
            guardianElderRelationMapper.updateById(inactiveRelation);
            log.info("监护关系恢复成功 - id: {}", inactiveRelation.getId());
            // 通知老人
            sendBindNotification(guardianId, elderId, request.getRelationType());
            return inactiveRelation;
        }

        GuardianElderRelation relation = new GuardianElderRelation();
        relation.setGuardianId(guardianId);
        relation.setElderId(elderId);
        relation.setRelationType(request.getRelationType());
        relation.setStatus(RelationStatus.ACTIVE.getCode());

        guardianElderRelationMapper.insert(relation);

        log.info("监护关系绑定成功 - id: {}", relation.getId());
        // 通知老人
        sendBindNotification(guardianId, elderId, request.getRelationType());
        return relation;
    }

    @Override
    public void unbindRelation(Long guardianId, Long elderId) {
        log.info("解绑监护关系 - guardianId: {}, elderId: {}", guardianId, elderId);

        LambdaQueryWrapper<GuardianElderRelation> query = new LambdaQueryWrapper<>();
        query.eq(GuardianElderRelation::getGuardianId, guardianId)
                .eq(GuardianElderRelation::getElderId, elderId)
                .eq(GuardianElderRelation::getStatus, RelationStatus.ACTIVE.getCode());
        GuardianElderRelation relation = guardianElderRelationMapper.selectOne(query);

        if (relation == null) {
            throw new BusinessException("未找到有效的监护关系");
        }

        relation.setStatus(RelationStatus.INACTIVE.getCode());
        guardianElderRelationMapper.updateById(relation);

        log.info("监护关系解绑成功 - guardianId: {}, elderId: {}", guardianId, elderId);
    }

    @Override
    public List<ExpiringDrugDTO> getExpiringDrugs(Long elderId) {
        log.info("获取老人临期药品 - elderId: {}", elderId);

        LocalDate threshold = LocalDate.now().plusDays(30);

        LambdaQueryWrapper<UserMedicineBox> query = new LambdaQueryWrapper<>();
        query.eq(UserMedicineBox::getUserId, elderId)
                .eq(UserMedicineBox::getStatus, UserMedicineBox.Status.ACTIVE.getCode())
                .le(UserMedicineBox::getExpiryDate, threshold)
                .ge(UserMedicineBox::getExpiryDate, LocalDate.now());
        List<UserMedicineBox> boxes = userMedicineBoxMapper.selectList(query);
        if (boxes.isEmpty()) {
            log.info("未找到临期药品 - elderId: {}", elderId);
            return new ArrayList<>();
        }

        // 批量查询药品名称
        List<Long> drugIds = boxes.stream()
                .map(UserMedicineBox::getDrugId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, DrugBase> drugMap = drugIds.isEmpty() ? Map.of()
                : drugBaseMapper.selectBatchIds(drugIds).stream()
                        .collect(Collectors.toMap(DrugBase::getId, d -> d));

        List<ExpiringDrugDTO> result = new ArrayList<>();
        for (UserMedicineBox box : boxes) {
            String drugName = "未知药品";
            DrugBase drug = drugMap.get(box.getDrugId());
            if (drug != null) {
                drugName = drug.getGenericName();
            }

            int daysUntilExpiry = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), box.getExpiryDate());

            result.add(ExpiringDrugDTO.builder()
                    .boxItemId(box.getId())
                    .drugId(box.getDrugId())
                    .drugName(drugName)
                    .expiryDate(box.getExpiryDate())
                    .remainingQuantity(box.getRemainingQuantity())
                    .daysUntilExpiry(daysUntilExpiry)
                    .build());
        }

        log.info("找到 {} 个临期药品 - elderId: {}", result.size(), elderId);
        return result;
    }

    /**
     * 构建老人摘要信息
     */
    private ElderSummaryDTO buildElderSummary(Long elderId) {
        // 查询老人基本信息
        LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(SysUser::getId, elderId);
        SysUser elder = userMapper.selectOne(userQuery);

        if (elder == null) {
            log.warn("未找到老人 - elderId: {}", elderId);
            return null;
        }

        // 查询今日用药计划统计
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<MedicationPlan> planQuery = new LambdaQueryWrapper<>();
        planQuery.eq(MedicationPlan::getUserId, elderId)
                .eq(MedicationPlan::getPlanDate, today);
        List<MedicationPlan> todayPlans = medicationPlanMapper.selectList(planQuery);

        int pendingCount = 0;
        int takenCount = 0;
        int missedCount = 0;
        for (MedicationPlan plan : todayPlans) {
            String planStatus = plan.getStatus();
            if (MedicationPlan.Status.PENDING.getCode().equals(planStatus)) {
                pendingCount++;
            } else if (MedicationPlan.Status.TAKEN.getCode().equals(planStatus)) {
                takenCount++;
            } else if (MedicationPlan.Status.MISSED.getCode().equals(planStatus)) {
                missedCount++;
            }
        }

        // 查询活跃告警数（待处理的紧急事件）
        LambdaQueryWrapper<EmergencyEvent> eventQuery = new LambdaQueryWrapper<>();
        eventQuery.eq(EmergencyEvent::getElderId, elderId)
                .eq(EmergencyEvent::getIsResolved, 0);
        Long alertCount = emergencyEventMapper.selectCount(eventQuery);

        // 查询用药数量（药箱中active状态的药品数）
        LambdaQueryWrapper<UserMedicineBox> boxQuery = new LambdaQueryWrapper<>();
        boxQuery.eq(UserMedicineBox::getUserId, elderId)
                .eq(UserMedicineBox::getStatus, UserMedicineBox.Status.ACTIVE.getCode());
        Long medicationCount = userMedicineBoxMapper.selectCount(boxQuery);

        // 最后活跃时间（优先使用 lastActiveTime，兜底使用 updatedAt）
        String lastActiveTime = formatLastActiveTime(elder.getLastActiveTime(), elder.getUpdatedAt(), LocalDateTime.now());

        return ElderSummaryDTO.builder()
                .elderId(elder.getId())
                .realName(elder.getRealName())
                .age(elder.getAge())
                .gender(elder.getGender())
                .medicationCount(medicationCount.intValue())
                .todayPendingCount(pendingCount)
                .todayTakenCount(takenCount)
                .todayMissedCount(missedCount)
                .activeAlertCount(alertCount.intValue())
                .lastActiveTime(lastActiveTime)
                .build();
    }

    @Override
    public ElderMedicationPlanDTO getMedicationPlan(Long elderId) {
        log.info("获取老人今日用药计划 - elderId: {}", elderId);

        SysUser elder = userMapper.selectById(elderId);
        String elderName = elder != null ? elder.getRealName() : "未知";

        LocalDate today = LocalDate.now();
        List<MedicationPlan> plans = medicationPlanMapper.selectUserDailyPlans(elderId, today);

        int takenCount = 0, pendingCount = 0, missedCount = 0, skippedCount = 0;
        List<ElderMedicationPlanItemDTO> items = new ArrayList<>();

        for (MedicationPlan plan : plans) {
            String drugName = "未知药品";
            String specification = "";
            if (plan.getDrugId() != null) {
                DrugBase drug = drugBaseMapper.selectById(plan.getDrugId());
                if (drug != null) {
                    drugName = drug.getGenericName();
                    specification = drug.getSpecification();
                }
            }

            String planStatus = plan.getStatus();
            if (MedicationPlan.Status.TAKEN.getCode().equals(planStatus) || "completed".equals(planStatus)) {
                takenCount++;
            } else if (MedicationPlan.Status.PENDING.getCode().equals(planStatus)) {
                pendingCount++;
            } else if (MedicationPlan.Status.MISSED.getCode().equals(planStatus)) {
                missedCount++;
            } else if (MedicationPlan.Status.SKIPPED.getCode().equals(planStatus)) {
                skippedCount++;
            }

            items.add(ElderMedicationPlanItemDTO.builder()
                    .planId(plan.getId())
                    .drugId(plan.getDrugId())
                    .drugName(drugName)
                    .specification(specification)
                    .dosageAtTime(plan.getDosageAtTime())
                    .timeSlot(plan.getTimeSlot())
                    .timeSlotLabel(getTimeSlotLabel(plan.getTimeSlot()))
                    .status(plan.getStatus())
                    .statusLabel(getStatusLabel(plan.getStatus()))
                    .build());
        }

        int totalCount = plans.size();
        int progressPercent = totalCount > 0 ? (takenCount + skippedCount) * 100 / totalCount : 0;

        return ElderMedicationPlanDTO.builder()
                .elderId(elderId)
                .elderName(elderName)
                .totalCount(totalCount)
                .takenCount(takenCount)
                .pendingCount(pendingCount)
                .missedCount(missedCount)
                .progressPercent(progressPercent)
                .items(items)
                .build();
    }

    private String getTimeSlotLabel(String timeSlot) {
        switch (timeSlot) {
            case "morning": return "早上";
            case "noon": return "中午";
            case "afternoon": return "下午";
            case "evening": return "晚上";
            case "before_bed": return "睡前";
            case "night": return "夜间";
            default: return timeSlot;
        }
    }

    private String getStatusLabel(String status) {
        switch (status) {
            case "pending": return "待服用";
            case "taken": return "已服用";
            case "missed": return "已漏服";
            case "skipped": return "已跳过";
            case "completed": return "已完成";
            default: return status;
        }
    }

    /**
     * 格式化最后活跃时间
     * 优先使用 lastActiveTime，兜底使用 updatedAt
     */
    private String formatLastActiveTime(LocalDateTime lastActiveTime, LocalDateTime updatedAt, LocalDateTime now) {
        LocalDateTime activeTime = lastActiveTime != null ? lastActiveTime : updatedAt;
        if (activeTime == null) {
            return null;
        }
        long minutes = java.time.Duration.between(activeTime, now).toMinutes();
        if (minutes < 0) {
            minutes = 0;
        }
        if (minutes == 0) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes < 1440) {
            return (minutes / 60) + "小时前";
        } else {
            return (minutes / 1440) + "天前";
        }
    }

    /**
     * 绑定成功后通知老人
     */
    private void sendBindNotification(Long guardianId, Long elderId, String relationType) {
        try {
            SysUser guardian = userMapper.selectById(guardianId);
            String guardianName = guardian != null ? guardian.getRealName() : "家属";
            String guardianPhone = guardian != null && guardian.getPhone() != null ? guardian.getPhone() : "";
            String relationship = relationType != null ? relationType : "家属";

            String title = "家属绑定通知";
            String content = guardianName + "（" + relationship + "）已绑定您，您可以选择将其添加为紧急联系人";

            // 构建extraData JSON
            Map<String, Object> extraData = new HashMap<>();
            extraData.put("guardianId", guardianId);
            extraData.put("guardianName", guardianName);
            extraData.put("guardianPhone", guardianPhone);
            extraData.put("relationship", relationship);

            String extraJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(extraData);
            elderNotificationService.createNotification(elderId, EventType.BIND_REQUEST.getCode(), title, content, extraJson);
            log.info("绑定通知已发送 - elderId: {}, guardianId: {}", elderId, guardianId);
        } catch (Exception e) {
            log.error("发送绑定通知失败 - elderId: {}, guardianId: {}", elderId, guardianId, e);
        }
    }
}
