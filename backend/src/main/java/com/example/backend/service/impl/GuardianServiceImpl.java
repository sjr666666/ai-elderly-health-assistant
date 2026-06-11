package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.*;
import com.example.backend.model.dto.ElderSummaryDTO;
import com.example.backend.model.dto.ExpiringDrugDTO;
import com.example.backend.model.dto.GuardianDashboardDTO;
import com.example.backend.model.dto.GuardianRelationRequest;
import com.example.backend.model.entity.*;
import com.example.backend.service.GuardianService;
import com.example.backend.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

        // 查询关联关系
        LambdaQueryWrapper<GuardianElderRelation> relationQuery = new LambdaQueryWrapper<>();
        relationQuery.eq(GuardianElderRelation::getGuardianId, guardianId)
                .eq(GuardianElderRelation::getStatus, "active");
        List<GuardianElderRelation> relations = guardianElderRelationMapper.selectList(relationQuery);

        List<ElderSummaryDTO> elderList = new ArrayList<>();
        for (GuardianElderRelation relation : relations) {
            ElderSummaryDTO elderDTO = buildElderSummary(relation.getElderId());
            if (elderDTO != null) {
                elderList.add(elderDTO);
            }
        }

        log.info("找到 {} 个关联老人 - guardianId: {}", elderList.size(), guardianId);
        return elderList;
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
                .eq(GuardianElderRelation::getStatus, "active");
        return guardianElderRelationMapper.selectCount(query) > 0;
    }

    @Override
    public GuardianElderRelation bindRelation(GuardianRelationRequest request) {
        log.info("绑定监护关系 - guardianId: {}, elderId: {}, elderUsername: {}",
                request.getGuardianId(), request.getElderId(), request.getElderUsername());

        Long elderId = request.getElderId();

        // 如果提供了elderUsername，先查找老人ID
        if (elderId == null && request.getElderUsername() != null) {
            LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
            userQuery.eq(SysUser::getUsername, request.getElderUsername())
                    .eq(SysUser::getRole, "elder");
            SysUser elder = userMapper.selectOne(userQuery);
            if (elder == null) {
                throw new RuntimeException("未找到该老人用户：" + request.getElderUsername());
            }
            elderId = elder.getId();
        }

        if (elderId == null) {
            throw new RuntimeException("请提供elderId或elderUsername");
        }

        // 检查是否已存在active关联
        LambdaQueryWrapper<GuardianElderRelation> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(GuardianElderRelation::getGuardianId, request.getGuardianId())
                .eq(GuardianElderRelation::getElderId, elderId)
                .eq(GuardianElderRelation::getStatus, "active");
        if (guardianElderRelationMapper.selectCount(existQuery) > 0) {
            throw new RuntimeException("监护关系已存在");
        }

        // 检查是否存在inactive关联（解绑过的），如果有则恢复
        LambdaQueryWrapper<GuardianElderRelation> inactiveQuery = new LambdaQueryWrapper<>();
        inactiveQuery.eq(GuardianElderRelation::getGuardianId, request.getGuardianId())
                .eq(GuardianElderRelation::getElderId, elderId)
                .eq(GuardianElderRelation::getStatus, "inactive");
        GuardianElderRelation inactiveRelation = guardianElderRelationMapper.selectOne(inactiveQuery);
        if (inactiveRelation != null) {
            inactiveRelation.setStatus("active");
            inactiveRelation.setRelationType(request.getRelationType());
            guardianElderRelationMapper.updateById(inactiveRelation);
            log.info("监护关系恢复成功 - id: {}", inactiveRelation.getId());
            return inactiveRelation;
        }

        GuardianElderRelation relation = new GuardianElderRelation();
        relation.setGuardianId(request.getGuardianId());
        relation.setElderId(elderId);
        relation.setRelationType(request.getRelationType());
        relation.setStatus("active");

        guardianElderRelationMapper.insert(relation);

        log.info("监护关系绑定成功 - id: {}", relation.getId());
        return relation;
    }

    @Override
    public void unbindRelation(Long guardianId, Long elderId) {
        log.info("解绑监护关系 - guardianId: {}, elderId: {}", guardianId, elderId);

        LambdaQueryWrapper<GuardianElderRelation> query = new LambdaQueryWrapper<>();
        query.eq(GuardianElderRelation::getGuardianId, guardianId)
                .eq(GuardianElderRelation::getElderId, elderId)
                .eq(GuardianElderRelation::getStatus, "active");
        GuardianElderRelation relation = guardianElderRelationMapper.selectOne(query);

        if (relation == null) {
            throw new RuntimeException("未找到有效的监护关系");
        }

        relation.setStatus("inactive");
        guardianElderRelationMapper.updateById(relation);

        log.info("监护关系解绑成功 - guardianId: {}, elderId: {}", guardianId, elderId);
    }

    @Override
    public List<ExpiringDrugDTO> getExpiringDrugs(Long elderId) {
        log.info("获取老人临期药品 - elderId: {}", elderId);

        LocalDate threshold = LocalDate.now().plusDays(30);

        LambdaQueryWrapper<UserMedicineBox> query = new LambdaQueryWrapper<>();
        query.eq(UserMedicineBox::getUserId, elderId)
                .eq(UserMedicineBox::getStatus, "active")
                .le(UserMedicineBox::getExpiryDate, threshold)
                .ge(UserMedicineBox::getExpiryDate, LocalDate.now());
        List<UserMedicineBox> boxes = userMedicineBoxMapper.selectList(query);

        List<ExpiringDrugDTO> result = new ArrayList<>();
        for (UserMedicineBox box : boxes) {
            String drugName = "未知药品";
            if (box.getDrugId() != null) {
                DrugBase drug = drugBaseMapper.selectById(box.getDrugId());
                if (drug != null) {
                    drugName = drug.getGenericName();
                }
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
            switch (plan.getStatus()) {
                case "pending":
                    pendingCount++;
                    break;
                case "taken":
                    takenCount++;
                    break;
                case "missed":
                    missedCount++;
                    break;
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
                .eq(UserMedicineBox::getStatus, "active");
        Long medicationCount = userMedicineBoxMapper.selectCount(boxQuery);

        // 最后活跃时间：使用用户最后更新时间
        String lastActiveTime = null;
        if (elder.getUpdatedAt() != null) {
            LocalDateTime updated = elder.getUpdatedAt();
            LocalDateTime now = LocalDateTime.now();
            long minutes = java.time.Duration.between(updated, now).toMinutes();
            if (minutes < 60) {
                lastActiveTime = minutes + "分钟前";
            } else if (minutes < 1440) {
                lastActiveTime = (minutes / 60) + "小时前";
            } else {
                lastActiveTime = (minutes / 1440) + "天前";
            }
        }

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
}
