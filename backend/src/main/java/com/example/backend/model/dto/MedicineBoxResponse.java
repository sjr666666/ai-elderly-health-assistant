package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 药箱列表响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineBoxResponse {

    /**
     * 药箱条目ID
     */
    private Long boxItemId;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 药品显示名称（优先显示俗名，俗名为空时显示通用名）
     */
    private String drugName;

    /**
     * 通用名（化学名）
     */
    private String genericName;

    /**
     * 商品名
     */
    private String tradeName;

    /**
     * 俗名/别名
     */
    private String commonName;

    /**
     * 药品规格
     */
    private String specification;

    /**
     * 每次用量
     */
    private String dosage;

    /**
     * 服用频率
     */
    private String frequency;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;

    /**
     * 药品有效期
     */
    private String expiryDate;

    /**
     * 总数量
     */
    private BigDecimal totalQuantity;

    /**
     * 剩余数量
     */
    private BigDecimal remainingQuantity;

    /**
     * 备注
     */
    private String note;

    /**
     * 药品名称（用于AI搜索或手动输入的药品，当drugId为空时使用）
     */
    private String aiDrugName;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private String createdAt;
}
