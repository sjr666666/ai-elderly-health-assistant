package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品冲突检测报告响应 DTO
 * 包含完整的检测报告信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugConflictResponse {

    /**
     * 检测报告ID
     */
    private String reportId;

    /**
     * 检测时间
     */
    private LocalDateTime checkTime;

    /**
     * 检测的药品列表
     */
    private List<String> drugsChecked;

    /**
     * 检测的保健品列表
     */
    private List<String> supplementsChecked;

    /**
     * 检测的饮料列表
     */
    private List<String> beveragesChecked;

    /**
     * 检测的食物列表
     */
    private List<String> foodsChecked;

    /**
     * 用户过敏史
     */
    private String allergyHistory;

    /**
     * 用户慢性病史
     */
    private String chronicDiseases;

    /**
     * 检测到的冲突列表
     */
    private List<DrugConflictResult> conflicts;

    /**
     * 是否存在严重冲突
     */
    private boolean hasSevereConflict;

    /**
     * 冲突数量统计
     */
    private ConflictStatistics statistics;

    /**
     * 总体用药建议
     */
    private String generalAdvice;

    /**
     * 检测是否完整
     */
    private boolean complete;

    /**
     * 冲突统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictStatistics {
        /**
         * 总冲突数
         */
        private int totalConflicts;

        /**
         * 重度冲突数
         */
        private int severeCount;

        /**
         * 中度冲突数
         */
        private int moderateCount;

        /**
         * 轻度冲突数
         */
        private int mildCount;
    }
}