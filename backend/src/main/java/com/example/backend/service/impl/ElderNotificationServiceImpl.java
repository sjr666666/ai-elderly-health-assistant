package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.mapper.ElderNotificationMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.ElderNotificationDTO;
import com.example.backend.model.entity.ElderNotification;
import com.example.backend.model.entity.EmergencyContact;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.EmergencyContactService;
import com.example.backend.service.ElderNotificationService;
import com.example.backend.service.NotificationOutboxService;
import com.example.backend.websocket.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 老人端通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElderNotificationServiceImpl implements ElderNotificationService {

    private final ElderNotificationMapper elderNotificationMapper;
    private final EmergencyContactService emergencyContactService;
    private final UserMapper userMapper;
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final NotificationOutboxService notificationOutboxService;

    @Override
    public ElderNotificationDTO createNotification(Long elderId, String notificationType, String title, String content, String extraData) {
        ElderNotification notification = new ElderNotification();
        notification.setElderId(elderId);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setExtraData(extraData);
        notification.setIsRead(0);
        notification.setIsHandled(0);

        elderNotificationMapper.insert(notification);
        log.info("创建老人通知 - elderId: {}, type: {}, id: {}", elderId, notificationType, notification.getId());

        // 通过WebSocket实时推送
        try {
            ElderNotificationDTO dto = toDTO(notification);
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "new_notification",
                    "data", dto
            ));
            notificationOutboxService.enqueue(notificationType, notification.getId(), elderId, message);
            log.info("WebSocket推送通知成功 - elderId: {}", elderId);
        } catch (Exception e) {
            log.warn("WebSocket推送通知失败 - elderId: {}, error: {}", elderId, e.getMessage());
        }

        return toDTO(notification);
    }

    @Override
    public List<ElderNotificationDTO> getNotifications(Long elderId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        LambdaQueryWrapper<ElderNotification> query = new LambdaQueryWrapper<>();
        query.eq(ElderNotification::getElderId, elderId)
                .orderByDesc(ElderNotification::getCreatedAt);
        Page<ElderNotification> page = new Page<>(1, limit, false);
        Page<ElderNotification> pageResult = elderNotificationMapper.selectPage(page, query);
        return pageResult.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public int getUnreadCount(Long elderId) {
        LambdaQueryWrapper<ElderNotification> query = new LambdaQueryWrapper<>();
        query.eq(ElderNotification::getElderId, elderId)
                .eq(ElderNotification::getIsRead, 0);
        return Math.toIntExact(elderNotificationMapper.selectCount(query));
    }

    @Override
    public void markAsRead(Long notificationId) {
        ElderNotification notification = elderNotificationMapper.selectById(notificationId);
        if (notification != null && notification.getIsRead() == 0) {
            notification.setIsRead(1);
            elderNotificationMapper.updateById(notification);
        }
    }

    @Override
    public void markAllAsRead(Long elderId) {
        LambdaUpdateWrapper<ElderNotification> update = new LambdaUpdateWrapper<>();
        update.eq(ElderNotification::getElderId, elderId)
                .eq(ElderNotification::getIsRead, 0)
                .set(ElderNotification::getIsRead, 1);
        elderNotificationMapper.update(null, update);
    }

    @Override
    public EmergencyContact addEmergencyContactFromNotification(Long notificationId) {
        ElderNotification notification = elderNotificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        if (!"bind_request".equals(notification.getNotificationType())) {
            throw new RuntimeException("该通知类型不支持添加紧急联系人");
        }
        if (notification.getIsHandled() == 1) {
            throw new RuntimeException("该通知已处理");
        }

        // 解析extraData获取家属信息
        try {
            Map<String, Object> extra = objectMapper.readValue(notification.getExtraData(), Map.class);
            Long guardianId = Long.valueOf(extra.get("guardianId").toString());
            String guardianName = (String) extra.get("guardianName");
            String guardianPhone = extra.get("guardianPhone") != null ? (String) extra.get("guardianPhone") : "";
            String relationship = (String) extra.get("relationship");

            // 创建紧急联系人
            EmergencyContact contact = new EmergencyContact();
            contact.setElderId(notification.getElderId());
            contact.setName(guardianName);
            contact.setPhone(guardianPhone);
            contact.setRelationship(relationship);

            // 检查是否已有主要联系人
            EmergencyContact existingPrimary = emergencyContactService.getContactByElderId(notification.getElderId());
            contact.setIsPrimary(existingPrimary == null ? 1 : 0);

            EmergencyContact saved = emergencyContactService.saveContact(contact);

            // 标记通知为已处理
            notification.setIsHandled(1);
            notification.setIsRead(1);
            elderNotificationMapper.updateById(notification);

            log.info("从通知添加紧急联系人成功 - notificationId: {}, contactId: {}", notificationId, saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("从通知添加紧急联系人失败 - notificationId: {}", notificationId, e);
            throw new RuntimeException("添加紧急联系人失败：" + e.getMessage());
        }
    }

    @Override
    public void updateEmergencyContactPhoneFromNotification(Long notificationId) {
        ElderNotification notification = elderNotificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        if (!"phone_update".equals(notification.getNotificationType())) {
            throw new RuntimeException("该通知类型不支持更新电话");
        }
        if (notification.getIsHandled() == 1) {
            throw new RuntimeException("该通知已处理");
        }

        try {
            Map<String, Object> extra = objectMapper.readValue(notification.getExtraData(), Map.class);
            String guardianName = (String) extra.get("guardianName");
            String newPhone = (String) extra.get("newPhone");

            // 查找老人紧急联系人中匹配该家属姓名的记录，更新电话
            List<EmergencyContact> contacts = emergencyContactService.getContactsByElderId(notification.getElderId());
            boolean updated = false;
            for (EmergencyContact contact : contacts) {
                if (guardianName.equals(contact.getName())) {
                    contact.setPhone(newPhone);
                    emergencyContactService.updateContact(contact);
                    updated = true;
                    log.info("更新紧急联系人电话 - contactId: {}, newPhone: {}", contact.getId(), newPhone);
                }
            }

            if (!updated) {
                log.warn("未找到匹配的紧急联系人 - elderId: {}, guardianName: {}", notification.getElderId(), guardianName);
            }

            // 标记通知为已处理
            notification.setIsHandled(1);
            notification.setIsRead(1);
            elderNotificationMapper.updateById(notification);
        } catch (Exception e) {
            log.error("从通知更新紧急联系人电话失败 - notificationId: {}", notificationId, e);
            throw new RuntimeException("更新电话失败：" + e.getMessage());
        }
    }

    @Override
    public int deleteReadNotifications(Long elderId) {
        LambdaQueryWrapper<ElderNotification> query = new LambdaQueryWrapper<>();
        query.eq(ElderNotification::getElderId, elderId)
                .eq(ElderNotification::getIsRead, 1);
        int count = Math.toIntExact(elderNotificationMapper.selectCount(query));
        elderNotificationMapper.delete(query);
        log.info("删除已读通知 - elderId: {}, count: {}", elderId, count);
        return count;
    }

    private ElderNotificationDTO toDTO(ElderNotification notification) {
        return ElderNotificationDTO.builder()
                .id(notification.getId())
                .elderId(notification.getElderId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .extraData(notification.getExtraData())
                .isRead(notification.getIsRead())
                .isHandled(notification.getIsHandled())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
