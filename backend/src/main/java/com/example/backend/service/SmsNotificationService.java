package com.example.backend.service;

import com.example.backend.model.dto.SmsNotificationDTO;

import java.util.List;

/**
 * 短信通知服务接口
 */
public interface SmsNotificationService {

    /**
     * 获取通知记录历史
     *
     * @param guardianId 监护人ID
     * @param limit      返回数量限制
     * @return 短信通知DTO列表
     */
    List<SmsNotificationDTO> getNotificationHistory(Long guardianId, Integer limit);

    /**
     * 发送通知
     *
     * @param guardianId 监护人ID
     * @param elderId    老人ID
     * @param eventType  事件类型
     * @param message    通知内容
     * @param phone      接收手机号
     */
    void sendNotification(Long guardianId, Long elderId, String eventType, String message, String phone);

    /**
     * 获取未读通知数量
     *
     * @param guardianId 监护人ID
     * @return 未读数量
     */
    int getUnreadCount(Long guardianId);

    /**
     * 标记所有通知为已读
     *
     * @param guardianId 监护人ID
     */
    void markAllAsRead(Long guardianId);

    /**
     * 标记单条通知为已读
     *
     * @param guardianId     监护人ID（归属校验）
     * @param notificationId 通知ID
     */
    void markAsRead(Long guardianId, Long notificationId);

    /**
     * 删除已读通知
     *
     * @param guardianId 监护人ID
     * @return 删除数量
     */
    int deleteReadNotifications(Long guardianId);
}
