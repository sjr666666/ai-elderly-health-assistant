package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 临期药品DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiringDrugDTO {

    /**
     * 药箱条目ID
     */
    private Long boxItemId;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 药品名称
     */
    private String drugName;

    /**
     * 有效期
     */
    private LocalDate expiryDate;

    /**
     * 剩余数量
     */
    private Integer remainingQuantity;

    /**
     * 距过期天数
     */
    private Integer daysUntilExpiry;
}
