package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.mapper.EmergencyEventMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.EmergencyEventDTO;
import com.example.backend.model.entity.EmergencyEvent;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.EmergencyEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 紧急事件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyEventServiceImpl implements EmergencyEventService {

    private final EmergencyEventMapper emergencyEventMapper;
    private final UserMapper userMapper;

    @Override
    public List<EmergencyEventDTO> getEventsByElderId(Long elderId, Integer limit) {
        log.info("获取老人紧急事件列表 - elderId: {}, limit: {}", elderId, limit);

        SysUser elder = userMapper.selectById(elderId);
        String elderName = elder != null ? elder.getRealName() : "未知";

        LambdaQueryWrapper<EmergencyEvent> query = new LambdaQueryWrapper<>();
        query.eq(EmergencyEvent::getElderId, elderId)
                .orderByDesc(EmergencyEvent::getCreatedAt)
                .last("LIMIT " + limit);
        List<EmergencyEvent> events = emergencyEventMapper.selectList(query);

        List<EmergencyEventDTO> result = new ArrayList<>();
        for (EmergencyEvent event : events) {
            result.add(EmergencyEventDTO.builder()
                    .eventId(event.getId())
                    .elderId(event.getElderId())
                    .elderName(elderName)
                    .eventType(event.getEventType())
                    .severity(event.getSeverity())
                    .description(event.getDescription())
                    .eventTime(event.getEventTime())
                    .status(event.getIsResolved() != null && event.getIsResolved() == 1 ? "resolved" : "pending")
                    .resolvedBy(event.getResolvedBy())
                    .resolvedAt(event.getResolvedAt())
                    .createdAt(event.getCreatedAt())
                    .build());
        }

        log.info("找到 {} 个紧急事件 - elderId: {}", result.size(), elderId);
        return result;
    }

    @Override
    public void resolveEvent(Long eventId, Long resolvedBy) {
        log.info("处理紧急事件 - eventId: {}, resolvedBy: {}", eventId, resolvedBy);

        LambdaUpdateWrapper<EmergencyEvent> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(EmergencyEvent::getId, eventId)
                .set(EmergencyEvent::getIsResolved, 1)
                .set(EmergencyEvent::getResolvedBy, resolvedBy)
                .set(EmergencyEvent::getResolvedAt, LocalDateTime.now());

        int rows = emergencyEventMapper.update(null, updateWrapper);
        if (rows == 0) {
            throw new RuntimeException("未找到紧急事件 - eventId: " + eventId);
        }

        log.info("紧急事件处理成功 - eventId: {}", eventId);
    }

    @Override
    public EmergencyEvent createEvent(EmergencyEvent event) {
        log.info("创建紧急事件 - elderId: {}, eventType: {}", event.getElderId(), event.getEventType());

        event.setIsResolved(0);
        emergencyEventMapper.insert(event);

        log.info("紧急事件创建成功 - id: {}", event.getId());
        return event;
    }
}
