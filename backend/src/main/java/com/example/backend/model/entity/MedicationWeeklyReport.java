package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用药周报实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("medication_weekly_report")
public class MedicationWeeklyReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 报告唯一标识（UUID）
     */
    @TableField("report_id")
    private String reportId;

    /**
     * 用户ID（关联sys_user.user_id）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 周报起始日期
     */
    @TableField("start_date")
    private LocalDate startDate;

    /**
     * 周报结束日期
     */
    @TableField("end_date")
    private LocalDate endDate;

    /**
     * 总体统计数据JSON
     */
    @TableField("statistics_json")
    private String statisticsJson;

    /**
     * AI生成的用药总结和建议
     */
    @TableField("ai_summary")
    private String aiSummary;

    /**
     * 完整报告文本（用于截图展示）
     */
    @TableField("full_report_text")
    private String fullReportText;

    /**
     * 总计划数
     */
    @TableField("total_plans")
    private Integer totalPlans;

    /**
     * 已服用数
     */
    @TableField("taken_count")
    private Integer takenCount;

    /**
     * 漏服数
     */
    @TableField("missed_count")
    private Integer missedCount;

    /**
     * 跳过数
     */
    @TableField("skipped_count")
    private Integer skippedCount;

    /**
     * 按时服药率（%）
     */
    @TableField("compliance_rate")
    private BigDecimal complianceRate;

    /**
     * 涉及药品种类数
     */
    @TableField("drug_variety_count")
    private Integer drugVarietyCount;

    /**
     * 表现最好的时段
     */
    @TableField("best_time_slot")
    private String bestTimeSlot;

    /**
     * 需要改进的时段
     */
    @TableField("needs_improvement_time_slot")
    private String needsImprovementTimeSlot;

    /**
     * 漏服药品列表JSON
     */
    @TableField("missed_drugs_json")
    private String missedDrugsJson;

    /**
     * 每日汇总详情JSON
     */
    @TableField("daily_summaries_json")
    private String dailySummariesJson;

    /**
     * 报告生成时间
     */
    @TableField("generated_at")
    private LocalDateTime generatedAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
