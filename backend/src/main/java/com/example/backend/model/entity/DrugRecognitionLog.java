package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 药品识别日志实体类
 * 记录药品识别与入库的完整日志，便于追踪和排查名称匹配问题
 */
@Data
@TableName("drug_recognition_log")
public class DrugRecognitionLog {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的OCR记录ID
     */
    @TableField("ocr_record_id")
    private Long ocrRecordId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 识别原始文本
     */
    @TableField("raw_text")
    private String rawText;

    /**
     * 标准化后的药品名称
     */
    @TableField("normalized_name")
    private String normalizedName;

    /**
     * 匹配到的药品ID（如果匹配成功）
     */
    @TableField("matched_drug_id")
    private Long matchedDrugId;

    /**
     * 匹配到的药品名称
     */
    @TableField("matched_drug_name")
    private String matchedDrugName;

    /**
     * 匹配分数
     */
    @TableField("match_score")
    private java.math.BigDecimal matchScore;

    /**
     * 是否匹配成功
     */
    @TableField("matched")
    private Boolean matched;

    /**
     * 是否自动入库
     */
    @TableField("auto_imported")
    private Boolean autoImported;

    /**
     * 新入库的药品ID（如果自动入库）
     */
    @TableField("imported_drug_id")
    private Long importedDrugId;

    /**
     * 处理状态：pending(待处理), matched(已匹配), unmatched(未匹配), imported(已入库)
     */
    @TableField("status")
    private String status;

    /**
     * 备注信息（如匹配失败原因）
     */
    @TableField("remark")
    private String remark;

    /**
     * 识别时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 状态枚举
     */
    public enum Status {
        PENDING("pending", "待处理"),
        MATCHED("matched", "已匹配"),
        UNMATCHED("unmatched", "未匹配"),
        IMPORTED("imported", "已入库");

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
    }
}