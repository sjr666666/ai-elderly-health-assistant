package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.mapper.SmsNotificationLogMapper;
import com.example.backend.model.dto.SmsNotificationDTO;
import com.example.backend.model.entity.SmsNotificationLog;
import com.example.backend.model.enums.SmsStatus;
import com.example.backend.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 短信通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsNotificationServiceImpl implements SmsNotificationService {

    private final SmsNotificationLogMapper smsNotificationLogMapper;

    @Override
    public List<SmsNotificationDTO> getNotificationHistory(Long guardianId, Integer limit) {
        log.info("获取短信通知记录 - guardianId: {}, limit: {}", guardianId, limit);

        LambdaQueryWrapper<SmsNotificationLog> query = new LambdaQueryWrapper<>();
        query.eq(SmsNotificationLog::getGuardianId, guardianId)
                .orderByDesc(SmsNotificationLog::getCreatedAt)
                .last("LIMIT " + limit);
        List<SmsNotificationLog> logs = smsNotificationLogMapper.selectList(query);

        List<SmsNotificationDTO> result = new ArrayList<>();
        for (SmsNotificationLog logEntry : logs) {
            result.add(SmsNotificationDTO.builder()
                    .id(logEntry.getId())
                    .guardianId(logEntry.getGuardianId())
                    .elderId(logEntry.getElderId())
                    .eventType(logEntry.getSmsType())
                    .message(logEntry.getContent())
                    .phone(logEntry.getPhone())
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
        log.info("发送短信通知 - guardianId: {}, elderId: {}, eventType: {}", guardianId, elderId, eventType);

        SmsNotificationLog notificationLog = new SmsNotificationLog();
        notificationLog.setGuardianId(guardianId);
        notificationLog.setElderId(elderId);
        notificationLog.setSmsType(eventType);
        notificationLog.setContent(message);
        notificationLog.setPhone(phone);
        notificationLog.setStatus(SmsStatus.PENDING.getCode());
        notificationLog.setRetryCount(0);
        notificationLog.setIsRead(0);

        smsNotificationLogMapper.insert(notificationLog);

        log.info("短信通知已记录 - id: {}, 待发送至: {}", notificationLog.getId(), phone);

        // 模拟发送成功
        notificationLog.setStatus(SmsStatus.SENT.getCode());
        notificationLog.setSentAt(LocalDateTime.now());
        smsNotificationLogMapper.updateById(notificationLog);

        log.info("短信通知发送成功 - id: {}", notificationLog.getId());
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
    public int deleteReadNotifications(Long guardianId) {
        LambdaQueryWrapper<SmsNotificationLog> query = new LambdaQueryWrapper<>();
        query.eq(SmsNotificationLog::getGuardianId, guardianId)
                .eq(SmsNotificationLog::getIsRead, 1);
        int count = smsNotificationLogMapper.delete(query);
        log.info("删除已读通知 - guardianId: {}, count: {}", guardianId, count);
        return count;
    }
}
