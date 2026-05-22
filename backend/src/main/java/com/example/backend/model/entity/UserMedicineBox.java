package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭药箱实体类
 * 对应数据库表：user_medicine_box
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@TableName("user_medicine_box")
public class UserMedicineBox {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属老人ID
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
     * 每次用量
     * 如"一片"、"半片"，用户自定义的用量描述
     */
    @TableField("dosage")
    private String dosage;

    /**
     * 频率
     * 如"每日两次"，用药频率描述
     */
    @TableField("frequency")
    private String frequency;

    /**
     * 开始服用日期
     * 用户开始服用该药品的日期
     */
    @TableField("start_date")
    private LocalDate startDate;

    /**
     * 预计结束日期
     * 用户预计停止服用的日期
     */
    @TableField("end_date")
    private LocalDate endDate;

    /**
     * 药品有效期
     * 药品本身的有效期截止日期
     */
    @TableField("expiry_date")
    private LocalDate expiryDate;

    /**
     * 总数量
     * 用户添加时填写的总片数/总瓶数
     */
    @TableField("total_quantity")
    private Integer totalQuantity;

    /**
     * 剩余数量
     * 根据服药记录自动扣减
     */
    @TableField("remaining_quantity")
    private Integer remainingQuantity;

    /**
     * 用户备注
     * 用户添加的额外说明或注意事项
     */
    @TableField("note")
    private String note;

    /**
     * 状态
     * active（使用中）/ stopped（已停用）
     * 默认值：active
     */
    @TableField("status")
    private String status;

    /**
     * 添加时间
     * 记录创建时自动生成
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 药箱状态枚举
     */
    public enum Status {
        ACTIVE("active", "使用中"),
        STOPPED("stopped", "已停用");

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
            return ACTIVE;
        }
    }
}
