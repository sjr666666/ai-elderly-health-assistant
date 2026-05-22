package com.example.backend.model.dto;

import lombok.Data;
import java.util.List;

/**
 * 添加药品到用药计划的请求DTO
 */
@Data
public class AddToPlanRequest {
    /**
     * 用户ID（雪花算法生成的ID）
     */
    private Long userId;

    /**
     * 药箱条目ID
     */
    private Long boxItemId;

    /**
     * 选择的时间段列表
     * 如：["morning", "evening"] 表示早上和晚上
     */
    private List<String> timeSlots;
}
