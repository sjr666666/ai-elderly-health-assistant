package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.*;
import com.example.backend.model.entity.GuardianElderRelation;
import com.example.backend.service.EmergencyEventService;
import com.example.backend.service.GuardianService;
import com.example.backend.service.MissedDoseMonitorService;
import com.example.backend.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/guardian")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;
    private final EmergencyEventService emergencyEventService;
    private final SmsNotificationService smsNotificationService;
    private final MissedDoseMonitorService missedDoseMonitorService;

    @GetMapping("/dashboard")
    public ResponseResult<GuardianDashboardDTO> getDashboard(@RequestParam Long guardianId) {
        log.info("获取监护人仪表盘数据 - guardianId: {}", guardianId);
        return ResponseResult.success(guardianService.getDashboard(guardianId));
    }

    @GetMapping("/elders")
    public ResponseResult<List<ElderSummaryDTO>> getElderList(@RequestParam Long guardianId) {
        log.info("获取关联老人列表 - guardianId: {}", guardianId);
        return ResponseResult.success(guardianService.getElderList(guardianId));
    }

    @GetMapping("/by-elder")
    public ResponseResult<List<GuardianSummaryDTO>> getGuardianList(@RequestParam Long elderId) {
        log.info("老人查询已绑定家属列表 - elderId: {}", elderId);
        return ResponseResult.success(guardianService.getGuardianList(elderId));
    }

    @GetMapping("/elders/{elderId}")
    public ResponseResult<ElderSummaryDTO> getElderDetail(@RequestParam Long guardianId, @PathVariable Long elderId) {
        log.info("获取老人详细信息 - guardianId: {}, elderId: {}", guardianId, elderId);
        if (!guardianService.hasPermission(guardianId, elderId)) {
            throw new com.example.backend.exception.BusinessException("无权访问该老人数据");
        }
        return ResponseResult.success(guardianService.getElderDetail(guardianId, elderId));
    }

    @PostMapping("/bind")
    public ResponseResult<GuardianElderRelation> bindRelation(@RequestBody GuardianRelationRequest request) {
        log.info("绑定监护关系 - guardianId: {}, elderId: {}", request.getGuardianId(), request.getElderId());
        return ResponseResult.success(guardianService.bindRelation(request));
    }

    @DeleteMapping("/unbind")
    public ResponseResult<Void> unbindRelation(@RequestParam Long guardianId, @RequestParam Long elderId) {
        log.info("解绑监护关系 - guardianId: {}, elderId: {}", guardianId, elderId);
        guardianService.unbindRelation(guardianId, elderId);
        return ResponseResult.success("解绑成功", null);
    }

    @GetMapping("/elders/{elderId}/events")
    public ResponseResult<List<EmergencyEventDTO>> getElderEvents(@RequestParam Long guardianId, @PathVariable Long elderId, @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取老人紧急事件列表 - guardianId: {}, elderId: {}, limit: {}", guardianId, elderId, limit);
        if (!guardianService.hasPermission(guardianId, elderId)) {
            throw new com.example.backend.exception.BusinessException("无权访问该老人数据");
        }
        return ResponseResult.success(emergencyEventService.getEventsByElderId(elderId, limit));
    }

    @PutMapping("/events/{eventId}/resolve")
    public ResponseResult<Void> resolveEvent(@PathVariable Long eventId, @RequestParam Long resolvedBy) {
        log.info("处理紧急事件 - eventId: {}, resolvedBy: {}", eventId, resolvedBy);
        emergencyEventService.resolveEvent(eventId, resolvedBy);
        return ResponseResult.success("事件已处理", null);
    }

    @GetMapping("/notifications")
    public ResponseResult<List<SmsNotificationDTO>> getNotifications(@RequestParam Long guardianId, @RequestParam(defaultValue = "20") Integer limit) {
        log.info("获取短信通知记录 - guardianId: {}, limit: {}", guardianId, limit);
        return ResponseResult.success(smsNotificationService.getNotificationHistory(guardianId, limit));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseResult<Integer> getUnreadCount(@RequestParam Long guardianId) {
        log.info("获取未读通知数量 - guardianId: {}", guardianId);
        return ResponseResult.success(smsNotificationService.getUnreadCount(guardianId));
    }

    @PutMapping("/notifications/read-all")
    public ResponseResult<Void> markAllAsRead(@RequestParam Long guardianId) {
        log.info("标记所有通知为已读 - guardianId: {}", guardianId);
        smsNotificationService.markAllAsRead(guardianId);
        return ResponseResult.success("标记成功", null);
    }

    @DeleteMapping("/notifications/read")
    public ResponseResult<String> deleteReadNotifications(@RequestParam Long guardianId) {
        log.info("清除已读通知 - guardianId: {}", guardianId);
        int count = smsNotificationService.deleteReadNotifications(guardianId);
        return ResponseResult.success("已清除" + count + "条已读通知");
    }

    @GetMapping("/elders/{elderId}/expiring-drugs")
    public ResponseResult<List<ExpiringDrugDTO>> getExpiringDrugs(@RequestParam Long guardianId, @PathVariable Long elderId) {
        log.info("获取老人临期药品 - guardianId: {}, elderId: {}", guardianId, elderId);
        if (!guardianService.hasPermission(guardianId, elderId)) {
            throw new com.example.backend.exception.BusinessException("无权访问该老人数据");
        }
        return ResponseResult.success(guardianService.getExpiringDrugs(elderId));
    }

    @GetMapping("/elders/{elderId}/medication-plan")
    public ResponseResult<ElderMedicationPlanDTO> getMedicationPlan(@RequestParam Long guardianId, @PathVariable Long elderId) {
        log.info("获取老人今日用药计划 - guardianId: {}, elderId: {}", guardianId, elderId);
        if (!guardianService.hasPermission(guardianId, elderId)) {
            throw new com.example.backend.exception.BusinessException("无权访问该老人数据");
        }
        return ResponseResult.success(guardianService.getMedicationPlan(elderId));
    }

    @PostMapping("/test/trigger-missed-check")
    @Profile("dev")
    public ResponseResult<Void> triggerMissedCheck() {
        log.info("手动触发漏服检查");
        missedDoseMonitorService.checkMissedDoses();
        return ResponseResult.success("漏服检查已触发", null);
    }
}
