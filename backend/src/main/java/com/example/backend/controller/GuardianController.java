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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 家属端控制器 - 所有接口需要JWT认证
 * guardianId从SecurityContext获取，不再接受前端传入
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/guardian")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;
    private final EmergencyEventService emergencyEventService;
    private final SmsNotificationService smsNotificationService;
    private final MissedDoseMonitorService missedDoseMonitorService;

    /**
     * 获取当前认证用户的ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }
        return (Long) authentication.getPrincipal();
    }

    @GetMapping("/dashboard")
    public ResponseResult<GuardianDashboardDTO> getDashboard() {
        Long guardianId = getCurrentUserId();
        log.info("获取监护人仪表盘数据 - guardianId: {}", guardianId);
        try {
            return ResponseResult.success(guardianService.getDashboard(guardianId));
        } catch (Exception e) {
            log.error("获取仪表盘数据失败 - guardianId: {}", guardianId, e);
            return ResponseResult.fail("获取仪表盘数据失败：" + e.getMessage());
        }
    }

    @GetMapping("/elders")
    public ResponseResult<List<ElderSummaryDTO>> getElderList() {
        Long guardianId = getCurrentUserId();
        log.info("获取关联老人列表 - guardianId: {}", guardianId);
        try {
            return ResponseResult.success(guardianService.getElderList(guardianId));
        } catch (Exception e) {
            log.error("获取老人列表失败 - guardianId: {}", guardianId, e);
            return ResponseResult.fail("获取老人列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/by-elder")
    public ResponseResult<List<GuardianSummaryDTO>> getGuardianList(@RequestParam Long elderId) {
        log.info("老人查询已绑定家属列表 - elderId: {}", elderId);
        try {
            return ResponseResult.success(guardianService.getGuardianList(elderId));
        } catch (Exception e) {
            log.error("获取家属列表失败 - elderId: {}", elderId, e);
            return ResponseResult.fail("获取家属列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/elders/{elderId}")
    public ResponseResult<ElderSummaryDTO> getElderDetail(@PathVariable Long elderId) {
        Long guardianId = getCurrentUserId();
        log.info("获取老人详细信息 - guardianId: {}, elderId: {}", guardianId, elderId);
        try {
            if (!guardianService.hasPermission(guardianId, elderId)) {
                return ResponseResult.fail("无权访问该老人数据");
            }
            return ResponseResult.success(guardianService.getElderDetail(guardianId, elderId));
        } catch (Exception e) {
            log.error("获取老人详细信息失败 - guardianId: {}, elderId: {}", guardianId, elderId, e);
            return ResponseResult.fail("获取老人详细信息失败：" + e.getMessage());
        }
    }

    @PostMapping("/bind")
    public ResponseResult<GuardianElderRelation> bindRelation(@RequestBody GuardianRelationRequest request) {
        Long guardianId = getCurrentUserId();
        log.info("绑定监护关系 - guardianId: {}, elderId: {}", guardianId, request.getElderId());
        try {
            return ResponseResult.success(guardianService.bindRelation(guardianId, request));
        } catch (Exception e) {
            log.error("绑定监护关系失败 - guardianId: {}, elderId: {}", guardianId, request.getElderId(), e);
            return ResponseResult.fail("绑定监护关系失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/unbind")
    public ResponseResult<Void> unbindRelation(@RequestParam Long elderId) {
        Long guardianId = getCurrentUserId();
        log.info("解绑监护关系 - guardianId: {}, elderId: {}", guardianId, elderId);
        try {
            guardianService.unbindRelation(guardianId, elderId);
            return ResponseResult.success("解绑成功", null);
        } catch (Exception e) {
            log.error("解绑监护关系失败 - guardianId: {}, elderId: {}", guardianId, elderId, e);
            return ResponseResult.fail("解绑监护关系失败：" + e.getMessage());
        }
    }

    @GetMapping("/elders/{elderId}/events")
    public ResponseResult<List<EmergencyEventDTO>> getElderEvents(@PathVariable Long elderId, @RequestParam(defaultValue = "10") Integer limit) {
        Long guardianId = getCurrentUserId();
        log.info("获取老人紧急事件列表 - guardianId: {}, elderId: {}, limit: {}", guardianId, elderId, limit);
        try {
            if (!guardianService.hasPermission(guardianId, elderId)) {
                return ResponseResult.fail("无权访问该老人数据");
            }
            return ResponseResult.success(emergencyEventService.getEventsByElderId(elderId, limit));
        } catch (Exception e) {
            log.error("获取紧急事件列表失败 - guardianId: {}, elderId: {}", guardianId, elderId, e);
            return ResponseResult.fail("获取紧急事件列表失败：" + e.getMessage());
        }
    }

    @PutMapping("/events/{eventId}/resolve")
    public ResponseResult<Void> resolveEvent(@PathVariable Long eventId) {
        Long guardianId = getCurrentUserId();
        log.info("处理紧急事件 - eventId: {}, guardianId: {}", eventId, guardianId);
        try {
            // 获取事件信息
            var event = emergencyEventService.getEventById(eventId);
            if (event == null) {
                return ResponseResult.fail("未找到紧急事件");
            }

            // 校验权限：只有关联家属才能处理事件
            if (!guardianService.hasPermission(guardianId, event.getElderId())) {
                log.warn("无权处理事件 - eventId: {}, guardianId: {}, elderId: {}", eventId, guardianId, event.getElderId());
                return ResponseResult.fail(403, "无权处理该事件");
            }

            emergencyEventService.resolveEvent(eventId, guardianId);
            return ResponseResult.success("事件已处理", null);
        } catch (Exception e) {
            log.error("处理紧急事件失败 - eventId: {}", eventId, e);
            return ResponseResult.fail("处理紧急事件失败：" + e.getMessage());
        }
    }

    @GetMapping("/notifications")
    public ResponseResult<List<SmsNotificationDTO>> getNotifications(@RequestParam(defaultValue = "20") Integer limit) {
        Long guardianId = getCurrentUserId();
        log.info("获取短信通知记录 - guardianId: {}, limit: {}", guardianId, limit);
        try {
            return ResponseResult.success(smsNotificationService.getNotificationHistory(guardianId, limit));
        } catch (Exception e) {
            log.error("获取短信通知记录失败 - guardianId: {}", guardianId, e);
            return ResponseResult.fail("获取短信通知记录失败：" + e.getMessage());
        }
    }

    @GetMapping("/notifications/unread-count")
    public ResponseResult<Integer> getUnreadCount() {
        Long guardianId = getCurrentUserId();
        log.info("获取未读通知数量 - guardianId: {}", guardianId);
        try {
            return ResponseResult.success(smsNotificationService.getUnreadCount(guardianId));
        } catch (Exception e) {
            log.error("获取未读通知数量失败 - guardianId: {}", guardianId, e);
            return ResponseResult.fail("获取未读通知数量失败：" + e.getMessage());
        }
    }

    @PutMapping("/notifications/read-all")
    public ResponseResult<Void> markAllAsRead() {
        Long guardianId = getCurrentUserId();
        log.info("标记所有通知为已读 - guardianId: {}", guardianId);
        try {
            smsNotificationService.markAllAsRead(guardianId);
            return ResponseResult.success("标记成功", null);
        } catch (Exception e) {
            log.error("标记已读失败 - guardianId: {}", guardianId, e);
            return ResponseResult.fail("标记已读失败：" + e.getMessage());
        }
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseResult<Void> markOneAsRead(@PathVariable Long id) {
        Long guardianId = getCurrentUserId();
        log.info("标记单条通知为已读 - guardianId: {}, notificationId: {}", guardianId, id);
        try {
            smsNotificationService.markAsRead(guardianId, id);
            return ResponseResult.success("标记成功", null);
        } catch (Exception e) {
            log.error("标记单条已读失败 - guardianId: {}, notificationId: {}", guardianId, id, e);
            return ResponseResult.fail("标记已读失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/notifications/read")
    public ResponseResult<String> deleteReadNotifications() {
        Long guardianId = getCurrentUserId();
        log.info("清除已读通知 - guardianId: {}", guardianId);
        try {
            int count = smsNotificationService.deleteReadNotifications(guardianId);
            return ResponseResult.success("已清除" + count + "条已读通知");
        } catch (Exception e) {
            log.error("清除已读通知失败 - guardianId: {}", guardianId, e);
            return ResponseResult.fail("清除失败：" + e.getMessage());
        }
    }

    @GetMapping("/elders/{elderId}/expiring-drugs")
    public ResponseResult<List<ExpiringDrugDTO>> getExpiringDrugs(@PathVariable Long elderId) {
        Long guardianId = getCurrentUserId();
        log.info("获取老人临期药品 - guardianId: {}, elderId: {}", guardianId, elderId);
        try {
            if (!guardianService.hasPermission(guardianId, elderId)) {
                return ResponseResult.fail("无权访问该老人数据");
            }
            return ResponseResult.success(guardianService.getExpiringDrugs(elderId));
        } catch (Exception e) {
            log.error("获取临期药品失败 - guardianId: {}, elderId: {}", guardianId, elderId, e);
            return ResponseResult.fail("获取临期药品失败：" + e.getMessage());
        }
    }

    @GetMapping("/elders/{elderId}/medication-plan")
    public ResponseResult<ElderMedicationPlanDTO> getMedicationPlan(@PathVariable Long elderId) {
        Long guardianId = getCurrentUserId();
        log.info("获取老人今日用药计划 - guardianId: {}, elderId: {}", guardianId, elderId);
        try {
            if (!guardianService.hasPermission(guardianId, elderId)) {
                return ResponseResult.fail("无权访问该老人数据");
            }
            return ResponseResult.success(guardianService.getMedicationPlan(elderId));
        } catch (Exception e) {
            log.error("获取用药计划失败 - guardianId: {}, elderId: {}", guardianId, elderId, e);
            return ResponseResult.fail("获取用药计划失败：" + e.getMessage());
        }
    }

    @PostMapping("/test/trigger-missed-check")
    public ResponseResult<Void> triggerMissedCheck() {
        log.info("手动触发漏服检查");
        try {
            missedDoseMonitorService.checkMissedDoses();
            return ResponseResult.success("漏服检查已触发", null);
        } catch (Exception e) {
            log.error("触发漏服检查失败", e);
            return ResponseResult.fail("触发漏服检查失败：" + e.getMessage());
        }
    }
}