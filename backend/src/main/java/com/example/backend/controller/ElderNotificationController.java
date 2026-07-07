package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.ElderNotificationDTO;
import com.example.backend.model.entity.EmergencyContact;
import com.example.backend.service.ElderNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 老人端通知控制器
 * elderId 从 SecurityContext 获取，不接受前端传入
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/elder/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ElderNotificationController {

    private final ElderNotificationService elderNotificationService;

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
     * 获取老人通知列表
     */
    @GetMapping
    public ResponseResult<List<ElderNotificationDTO>> getNotifications(
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        Long elderId = getCurrentUserId();
        log.info("获取老人通知列表 - elderId: {}, limit: {}", elderId, limit);
        return ResponseResult.success(elderNotificationService.getNotifications(elderId, limit));
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public ResponseResult<Integer> getUnreadCount() {
        Long elderId = getCurrentUserId();
        return ResponseResult.success(elderNotificationService.getUnreadCount(elderId));
    }

    /**
     * 标记单条通知为已读
     */
    @PutMapping("/{id}/read")
    public ResponseResult<String> markAsRead(@PathVariable("id") Long id) {
        elderNotificationService.markAsRead(id);
        return ResponseResult.success("已标记为已读");
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    public ResponseResult<String> markAllAsRead() {
        Long elderId = getCurrentUserId();
        elderNotificationService.markAllAsRead(elderId);
        return ResponseResult.success("已全部标记为已读");
    }

    /**
     * 从绑定通知添加紧急联系人
     */
    @PostMapping("/{id}/add-contact")
    public ResponseResult<EmergencyContact> addEmergencyContact(@PathVariable("id") Long id) {
        log.info("从通知添加紧急联系人 - notificationId: {}", id);
        EmergencyContact contact = elderNotificationService.addEmergencyContactFromNotification(id);
        return ResponseResult.success("添加成功", contact);
    }

    /**
     * 从电话变更通知更新紧急联系人电话
     */
    @PostMapping("/{id}/update-phone")
    public ResponseResult<String> updateEmergencyContactPhone(@PathVariable("id") Long id) {
        log.info("从通知更新紧急联系人电话 - notificationId: {}", id);
        elderNotificationService.updateEmergencyContactPhoneFromNotification(id);
        return ResponseResult.success("电话已更新");
    }

    /**
     * 清除已读通知
     */
    @DeleteMapping("/read")
    public ResponseResult<String> deleteReadNotifications() {
        Long elderId = getCurrentUserId();
        int count = elderNotificationService.deleteReadNotifications(elderId);
        return ResponseResult.success("已清除" + count + "条已读通知");
    }
}
