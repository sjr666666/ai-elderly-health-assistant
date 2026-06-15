package com.example.backend.service;

import com.example.backend.model.dto.ElderNotificationDTO;
import com.example.backend.model.entity.EmergencyContact;

import java.util.List;

/**
 * 老人端通知服务接口
 */
public interface ElderNotificationService {

    /**
     * 创建通知
     */
    ElderNotificationDTO createNotification(Long elderId, String notificationType, String title, String content, String extraData);

    /**
     * 获取老人通知列表
     */
    List<ElderNotificationDTO> getNotifications(Long elderId, Integer limit);

    /**
     * 获取未读通知数量
     */
    int getUnreadCount(Long elderId);

    /**
     * 标记单条通知为已读
     */
    void markAsRead(Long notificationId);

    /**
     * 标记所有通知为已读
     */
    void markAllAsRead(Long elderId);

    /**
     * 从绑定通知添加紧急联系人
     */
    EmergencyContact addEmergencyContactFromNotification(Long notificationId);

    /**
     * 从电话变更通知更新紧急联系人电话
     */
    void updateEmergencyContactPhoneFromNotification(Long notificationId);

    /**
     * 删除已读通知
     */
    int deleteReadNotifications(Long elderId);
}
