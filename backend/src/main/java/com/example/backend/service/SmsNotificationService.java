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
}
