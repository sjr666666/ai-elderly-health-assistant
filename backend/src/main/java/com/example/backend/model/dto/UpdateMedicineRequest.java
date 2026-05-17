package com.example.backend.model.dto;

import lombok.Data;

/**
 * 修改药箱条目请求DTO
 * 所有字段均为可选，支持部分字段更新
 */
@Data
public class UpdateMedicineRequest {

    /**
     * 每次用量（如"1片""5ml"）
     */
    private String dosage;

    /**
     * 服用频率（如"每日两次""睡前"）
     */
    private String frequency;

    /**
     * 开始服用日期（yyyy-MM-dd）
     */
    private String startDate;

    /**
     * 预计结束日期（yyyy-MM-dd）
     */
    private String endDate;

    /**
     * 药品有效期（yyyy-MM-dd）
     */
    private String expiryDate;

    /**
     * 总数量（如30片、60片）
     */
    private Integer totalQuantity;

    /**
     * 剩余数量
     */
    private Integer remainingQuantity;

    /**
     * 备注说明（最长500字符）
     */
    private String note;

    /**
     * 状态：active（使用中）/ stopped（已停用）
     */
    private String status;
}
