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
        try {
            return ResponseResult.success(guardianService.getDashboard(guardianId));
        } catch (Exception e) {
            log.error("获取仪表盘数据失败 - guardianId: {}", guardianId, e);
            return ResponseResult.fail("获取仪表盘数据失败：" + e.getMessage());
        }
    }

    @GetMapping("/elders")
    public ResponseResult<List<ElderSummaryDTO>> getElderList(@RequestParam Long guardianId) {
        log.info("获取关联老人列表 - guardianId: {}", guardianId);
        try {
            return ResponseResult.success(guardianService.getElderList(guardianId));
        } catch (Exception e) {
            log.error("获取老人列表失败 - guardianId: {}", guardianId, e);
            return ResponseResult.fail("获取老人列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/elders/{elderId}")
    public ResponseResult<ElderSummaryDTO> getElderDetail(@RequestParam Long guardianId, @PathVariable Long elderId) {
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
        log.info("绑定监护关系 - guardianId: {}, elderId: {}", request.getGuardianId(), request.getElderId());
        try {
            return ResponseResult.success(guardianService.bindRelation(request));
        } catch (Exception e) {
            log.error("绑定监护关系失败 - guardianId: {}, elderId: {}", request.getGuardianId(), request.getElderId(), e);
            return ResponseResult.fail("绑定监护关系失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/unbind")
    public ResponseResult<Void> unbindRelation(@RequestParam Long guardianId, @RequestParam Long elderId) {
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
    public ResponseResult<List<EmergencyEventDTO>> getElderEvents(@RequestParam Long guardianId, @PathVariable Long elderId, @RequestParam(defaultValue = "10") Integer limit) {
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
    public ResponseResult<Void> resolveEvent(@PathVariable Long eventId, @RequestParam Long resolvedBy) {
        log.info("处理紧急事件 - eventId: {}, resolvedBy: {}", eventId, resolvedBy);
        try {
            emergencyEventService.resolveEvent(eventId, resolvedBy);
            return ResponseResult.success("事件已处理", null);
        } catch (Exception e) {
            log.error("处理紧急事件失败 - eventId: {}", eventId, e);
            return ResponseResult.fail("处理紧急事件失败：" + e.getMessage());
        }
    }

    @GetMapping("/notifications")
    public ResponseResult<List<SmsNotificationDTO>> getNotifications(@RequestParam Long guardianId, @RequestParam(defaultValue = "20") Integer limit) {
        log.info("获取短信通知记录 - guardianId: {}, limit: {}", guardianId, limit);
        try {
            return ResponseResult.success(smsNotificationService.getNotificationHistory(guardianId, limit));
        } catch (Exception e) {
            log.error("获取短信通知记录失败 - guardianId: {}", guardianId, e);
            return ResponseResult.fail("获取短信通知记录失败：" + e.getMessage());
        }
    }

    @GetMapping("/elders/{elderId}/expiring-drugs")
    public ResponseResult<List<ExpiringDrugDTO>> getExpiringDrugs(@RequestParam Long guardianId, @PathVariable Long elderId) {
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
