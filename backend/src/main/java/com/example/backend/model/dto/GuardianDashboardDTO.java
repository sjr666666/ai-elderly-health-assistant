package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 监护人仪表盘DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianDashboardDTO {

    /**
     * 关联老人数量
     */
    private Integer elderCount;

    /**
     * 活跃告警数量
     */
    private Integer activeAlertCount;

    /**
     * 未读通知数量
     */
    private Integer unreadNotificationCount;

    /**
     * 关联老人摘要列表
     */
    private List<ElderSummaryDTO> elders;
}
