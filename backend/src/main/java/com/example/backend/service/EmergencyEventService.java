package com.example.backend.service;

import com.example.backend.model.dto.EmergencyEventDTO;
import com.example.backend.model.entity.EmergencyEvent;

import java.util.List;

/**
 * 紧急事件服务接口
 */
public interface EmergencyEventService {

    /**
     * 获取老人的紧急事件列表
     *
     * @param elderId 老人ID
     * @param limit   返回数量限制
     * @return 紧急事件DTO列表
     */
    List<EmergencyEventDTO> getEventsByElderId(Long elderId, Integer limit);

    /**
     * 处理紧急事件
     *
     * @param eventId    事件ID
     * @param resolvedBy 处理人ID
     */
    void resolveEvent(Long eventId, Long resolvedBy);

    /**
     * 创建紧急事件
     *
     * @param event 紧急事件实体
     * @return 创建后的紧急事件
     */
    EmergencyEvent createEvent(EmergencyEvent event);

    /**
     * 根据ID获取紧急事件
     *
     * @param eventId 事件ID
     * @return 紧急事件实体
     */
    EmergencyEvent getEventById(Long eventId);
}
