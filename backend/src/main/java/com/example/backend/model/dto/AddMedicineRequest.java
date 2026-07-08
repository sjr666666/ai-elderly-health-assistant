package com.example.backend.model.dto;

import com.example.backend.model.entity.UserMedicineBox;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 添加药品到药箱请求DTO
 */
@Data
public class AddMedicineRequest {

    /**
     * 药品基础库ID（AI搜索到的药品可能为空）
     */
    private Long drugId;

    /**
     * 每次用量（如"1片""5ml"）
     */
    @NotBlank(message = "每次用量不能为空")
    private String dosage;

    /**
     * 服用频率（如"每日两次""睡前"）
     */
    @NotBlank(message = "服用频率不能为空")
    private String frequency;

    /**
     * 开始服用日期（默认当前日期 yyyy-MM-dd）
     */
    private String startDate;

    /**
     * 预计结束日期
     */
    private String endDate;

    /**
     * 药品有效期（触发临期提醒的核心字段）
     */
    @NotBlank(message = "药品有效期不能为空")
    private String expiryDate;

    /**
     * 总数量（如30片、60片）
     */
    @NotNull(message = "总数量不能为空")
    private Integer totalQuantity;

    /**
     * 备注说明（最长500字符）
     */
    private String note;

    /**
     * 药品名称（AI搜索或手动输入的药品名称，用于在药品ID为空时显示）
     */
    private String drugName;

    /**
     * 药品名称（兼容字段，与drugName相同）
     */
    private String name;

    /**
     * 药品规格（如"0.25g*20片"）
     */
    private String spec;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 状态，默认 active
     */
    private String status = UserMedicineBox.Status.ACTIVE.getCode();
}
