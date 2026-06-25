package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 用药计划实体类
 * 对应数据库表：medication_plan
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medication_plan")
public class MedicationPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 老人ID
     * 关联sys_user表的主键
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 药品ID
     * 关联drug_base表的主键
     */
    @TableField("drug_id")
    private Long drugId;

    /**
     * 关联药箱条目ID
     * 关联user_medicine_box表的主键，可为空
     */
    @TableField("box_item_id")
    private Long boxItemId;

    /**
     * 计划日期
     * 用药计划的日期
     */
    @TableField("plan_date")
    private LocalDate planDate;

    /**
     * 时段
     * morning（上午）/ noon（中午）/ evening（晚上）/ before_bed（睡前）
     */
    @TableField("time_slot")
    private String timeSlot;

    /**
     * 该时段用量
     * 如"1片"，该时段需要服用的药量
     */
    @TableField("dosage_at_time")
    private String dosageAtTime;

    /**
     * 状态
     * pending（待服用）/ taken（已服用）/ missed（已错过）/ skipped（已跳过）
     * 默认值：pending
     */
    @TableField("status")
    private String status;

    /**
     * 提前提醒分钟数
     * 如15，表示提前15分钟提醒用户服药
     */
    @TableField("remind_before")
    private Integer remindBefore;

    /**
     * 提醒阶段
     * none（未触发）/ pre_remind（提前提醒）/ due_now（到时提醒）/ overdue（超时提醒）/ notify_family（已通知家属）
     * 默认值：none
     */
    @TableField("reminder_stage")
    private String reminderStage;

    /**
     * 时段枚举
     */
    public enum TimeSlot {
        MORNING("morning", "上午"),
        NOON("noon", "中午"),
        EVENING("evening", "晚上"),
        BEFORE_BED("before_bed", "睡前");

        private final String code;
        private final String description;

        TimeSlot(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static TimeSlot fromCode(String code) {
            for (TimeSlot slot : values()) {
                if (slot.code.equals(code)) {
                    return slot;
                }
            }
            return MORNING;
        }
    }

    /**
     * 用药状态枚举
     */
    public enum Status {
        PENDING("pending", "待服用"),
        TAKEN("taken", "已服用"),
        MISSED("missed", "已错过"),
        SKIPPED("skipped", "已跳过");

        private final String code;
        private final String description;

        Status(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static Status fromCode(String code) {
            for (Status status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            return PENDING;
        }
    }
}