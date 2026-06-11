package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 老人摘要信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElderSummaryDTO {

    /**
     * 老人ID
     */
    private Long elderId;

    /**
     * 老人姓名
     */
    private String realName;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别
     */
    private String gender;

    /**
     * 用药数量
     */
    private Integer medicationCount;

    /**
     * 今日待服用数量
     */
    private Integer todayPendingCount;

    /**
     * 今日已服用数量
     */
    private Integer todayTakenCount;

    /**
     * 今日漏服数量
     */
    private Integer todayMissedCount;

    /**
     * 活跃告警数量
     */
    private Integer activeAlertCount;
}
