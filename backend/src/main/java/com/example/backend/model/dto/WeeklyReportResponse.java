package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药周报响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReportResponse {
    
    /**
     * 报告ID
     */
    private String reportId;
    
    /**
     * 生成时间
     */
    private java.time.LocalDateTime generatedAt;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 周报起始日期
     */
    private LocalDate startDate;
    
    /**
     * 周报结束日期
     */
    private LocalDate endDate;
    
    /**
     * 总体统计信息
     */
    private ReportStatistics statistics;
    
    /**
     * 每日详情列表
     */
    private List<DailySummary> dailySummaries;
    
    /**
     * AI生成的用药总结和建议
     */
    private String aiSummary;
    
    /**
     * 漏服药品列表
     */
    private List<String> missedDrugs;
    
    /**
     * 按时服药率最高的时段
     */
    private String bestTimeSlot;
    
    /**
     * 需要改进的时段
     */
    private String needsImprovementTimeSlot;
    
    /**
     * 完整报告文本（用于截图展示）
     */
    private String fullReportText;
    
    /**
     * 总体统计数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportStatistics {
        /**
         * 总计划数
         */
        private Integer totalPlans;
        
        /**
         * 已服用数
         */
        private Integer takenCount;
        
        /**
         * 漏服数
         */
        private Integer missedCount;
        
        /**
         * 跳过数
         */
        private Integer skippedCount;
        
        /**
         * 待服用数
         */
        private Integer pendingCount;
        
        /**
         * 按时服药率
         */
        private Double complianceRate;
        
        /**
         * 涉及药品种类数
         */
        private Integer drugVarietyCount;
    }
    
    /**
     * 每日汇总
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySummary {
        /**
         * 日期
         */
        private LocalDate date;
        
        /**
         * 星期几
         */
        private String dayOfWeek;
        
        /**
         * 当日计划数
         */
        private Integer totalPlans;
        
        /**
         * 已服用数
         */
        private Integer takenCount;
        
        /**
         * 漏服数
         */
        private Integer missedCount;
        
        /**
         * 当日合规率
         */
        private Double complianceRate;
        
        /**
         * 当日药品列表
         */
        private List<String> drugs;
    }
}
