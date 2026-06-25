package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 家属摘要信息DTO
 * 用于老人端查询已绑定自己的家属列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianSummaryDTO {

    /**
     * 家属ID
     */
    private Long guardianId;

    /**
     * 家属姓名
     */
    private String realName;

    /**
     * 电话
     */
    private String phone;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别
     */
    private String gender;

    /**
     * 与老人的关系
     * 如儿子、女儿、配偶、护工等
     */
    private String relationType;

    /**
     * 最近活跃时间（格式化字符串，如"5分钟前"）
     */
    private String lastActiveTime;
}
