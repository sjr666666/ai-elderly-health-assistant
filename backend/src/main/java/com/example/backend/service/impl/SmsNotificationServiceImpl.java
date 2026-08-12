package com.example.backend.service.impl;

import com.example.backend.common.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.util.PhoneEncryptUtil;
import com.example.backend.mapper.SmsNotificationLogMapper;
import com.example.backend.model.dto.SmsNotificationDTO;
import com.example.backend.model.entity.SmsNotificationLog;
import com.example.backend.model.enums.SmsStatus;
import com.example.backend.service.SmsNotificationService;
import com.example.backend.service.sms.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 短信通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsNotificationServiceImpl implements SmsNotificationService {

    private final SmsNotificationLogMapper smsNotificationLogMapper;
    private final Map<String, SmsProvider> smsProviders;

    @Value("${phone.encrypt.key}")
    private String phoneEncryptKey;

    /** 短信通道：mock（模拟，默认）/ 后续接入 aliyun 等真实服务商 */
    @Value("${sms.provider:mock}")
    private String smsProviderName;

    @Override
    public List<SmsNotificationDTO> getNotificationHistory(Long guardianId, Integer limit) {
        log.info("获取短信通知记录 - guardianId: {}, limit: {}", guardianId, limit);

        LambdaQueryWrapper<SmsNotificationLog> query = new LambdaQueryWrapper<>();
        query.eq(SmsNotificationLog::getGuardianId, guardianId)
                .orderByDesc(SmsNotificationLog::getCreatedAt);
        Page<SmsNotificationLog> page = new Page<>(1, limit, false);
        Page<SmsNotificationLog> pageResult = smsNotificationLogMapper.selectPage(page, query);
        List<SmsNotificationLog> logs = pageResult.getRecords();

        List<SmsNotificationDTO> result = new ArrayList<>();
        for (SmsNotificationLog logEntry : logs) {
            result.add(SmsNotificationDTO.builder()
                    .id(logEntry.getId())
                    .guardianId(logEntry.getGuardianId())
                    .elderId(logEntry.getElderId())
                    .eventType(logEntry.getSmsType())
                    .message(logEntry.getContent())
                    .phone(PhoneEncryptUtil.mask(PhoneEncryptUtil.decrypt(logEntry.getPhone(), phoneEncryptKey)))
                    .sendStatus(logEntry.getStatus())
                    .sentAt(logEntry.getSentAt())
                    .createdAt(logEntry.getCreatedAt())
                    .isRead(logEntry.getIsRead() != null && logEntry.getIsRead() == 1)
                    .build());
        }

        log.info("找到 {} 条通知记录 - guardianId: {}", result.size(), guardianId);
        return result;
    }

    @Override
    public void sendNotification(Long guardianId, Long elderId, String eventType, String message, String phone) {
        log.info("发送短信通知 - guardianId: {}, elderId: {}, eventType: {}, channel: {}",
                guardianId, elderId, eventType, smsProviderName);

        SmsNotificationLog notificationLog = new SmsNotificationLog();
        notificationLog.setGuardianId(guardianId);
        notificationLog.setElderId(elderId);
        notificationLog.setSmsType(eventType);
        notificationLog.setContent(message);
        notificationLog.setPhone(PhoneEncryptUtil.encrypt(phone, phoneEncryptKey));
        notificationLog.setStatus(SmsStatus.PENDING.getCode());
        notificationLog.setRetryCount(0);
        notificationLog.setIsRead(0);

        smsNotificationLogMapper.insert(notificationLog);

        boolean sent = false;
        SmsProvider provider = smsProviders.get(smsProviderName);
        if (provider == null) {
            log.warn("短信通道 {} 不存在，回退 mock - id: {}", smsProviderName, notificationLog.getId());
            provider = smsProviders.get("mock");
        }
        try {
            sent = provider.send(phone, message);
        } catch (Exception e) {
            log.error("短信发送异常 - id: {}, channel: {}, error: {}",
                    notificationLog.getId(), provider.name(), e.getMessage());
        }

        notificationLog.setStatus(sent ? SmsStatus.SENT.getCode() : SmsStatus.FAILED.getCode());
        if (sent) {
            notificationLog.setSentAt(LocalDateTime.now());
        }
        smsNotificationLogMapper.updateById(notificationLog);

        log.info("短信通知处理完成 - id: {}, status: {}", notificationLog.getId(), notificationLog.getStatus());
    }

    @Override
    public int getUnreadCount(Long guardianId) {
        LambdaQueryWrapper<SmsNotificationLog> query = new LambdaQueryWrapper<>();
        query.eq(SmsNotificationLog::getGuardianId, guardianId)
                .and(w -> w.eq(SmsNotificationLog::getIsRead, 0).or().isNull(SmsNotificationLog::getIsRead));
        return Math.toIntExact(smsNotificationLogMapper.selectCount(query));
    }

    @Override
    public void markAllAsRead(Long guardianId) {
        log.info("标记所有通知为已读 - guardianId: {}", guardianId);
        LambdaUpdateWrapper<SmsNotificationLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SmsNotificationLog::getGuardianId, guardianId)
                .and(w -> w.eq(SmsNotificationLog::getIsRead, 0).or().isNull(SmsNotificationLog::getIsRead))
                .set(SmsNotificationLog::getIsRead, 1);
        smsNotificationLogMapper.update(null, updateWrapper);
    }

    @Override
    public void markAsRead(Long guardianId, Long notificationId) {
        log.info("标记单条通知为已读 - guardianId: {}, notificationId: {}", guardianId, notificationId);
        // 归属校验：确保该通知属于当前用户
        SmsNotificationLog notification = smsNotificationLogMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!notification.getGuardianId().equals(guardianId)) {
            throw new BusinessException("无权操作该通知");
        }
        // 标记已读
        notification.setIsRead(1);
        smsNotificationLogMapper.updateById(notification);
    }

    @Override
    public int deleteReadNotifications(Long guardianId) {
        LambdaQueryWrapper<SmsNotificationLog> query = new LambdaQueryWrapper<>();
        query.eq(SmsNotificationLog::getGuardianId, guardianId)
                .eq(SmsNotificationLog::getIsRead, 1);
        int count = smsNotificationLogMapper.delete(query);
        log.info("删除已读通知 - guardianId: {}, count: {}", guardianId, count);
        return count;
    }
}
