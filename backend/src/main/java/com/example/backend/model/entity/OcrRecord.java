package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OCR识别记录实体类
 * 对应数据库表：ocr_record
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ocr_record")
public class OcrRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 上传用户ID
     * 关联sys_user表的主键
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 图片存储路径
     * OCR识别的图片文件路径
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * OCR原始识别文本
     * OCR引擎识别出的原始文本内容
     */
    @TableField("raw_text")
    private String rawText;

    /**
     * 匹配到的药品ID
     * 关联drug_base表的主键，可为空表示未匹配到药品
     */
    @TableField("matched_drug_id")
    private Long matchedDrugId;

    /**
     * 匹配置信度
     * OCR识别结果与药品匹配的置信度，范围0-1
     */
    @TableField("match_score")
    private BigDecimal matchScore;

    /**
     * 状态
     * pending（待处理）/ matched（已匹配）/ unmatched（未匹配）/ failed（识别失败）
     * 默认值：pending
     */
    @TableField("status")
    private String status;

    /**
     * 上传时间
     * 记录创建时自动生成
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * OCR识别状态枚举
     */
    public enum Status {
        PENDING("pending", "待处理"),
        MATCHED("matched", "已匹配"),
        UNMATCHED("unmatched", "未匹配"),
        FAILED("failed", "识别失败");

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