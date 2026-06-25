package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药周报请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportRequest {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 周报起始日期（默认为7天前）
     */
    private LocalDate startDate;
    
    /**
     * 周报结束日期（默认为今天）
     */
    private LocalDate endDate;
}
