package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.backend.model.enums.RelationStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 监护关系实体类
 * 对应数据库表：guardian_elder_relation
 *
 * @author backend
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("guardian_elder_relation")
public class GuardianElderRelation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 监护人ID
     * 关联sys_user表的主键（角色为family的用户）
     */
    @TableField("guardian_id")
    private Long guardianId;

    /**
     * 老人ID
     * 关联sys_user表的主键（角色为elder的用户）
     */
    @TableField("elder_id")
    private Long elderId;

    /**
     * 关系类型
     * 如子女、配偶、护工等
     */
    @TableField("relation_type")
    private String relationType;

    /**
     * 状态
     * @see RelationStatus
     * 默认值：active
     */
    @TableField("status")
    private String status;
}
