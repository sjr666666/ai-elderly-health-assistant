package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 紧急联系人实体类
 * 对应数据库表：emergency_contact
 * 
 * @author backend
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("emergency_contact")
public class EmergencyContact extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 所属老人ID
     * 关联sys_user表的主键
     */
    @TableField("elder_id")
    private Long elderId;

    /**
     * 联系人姓名
     * 紧急联系人的姓名
     */
    @TableField("name")
    private String name;

    /**
     * 联系电话
     * 用于tel:协议的电话号码
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱
     * 用于通知的邮箱地址，可选字段
     */
    @TableField("email")
    private String email;

    /**
     * 关系
     * 如子女、护工等，描述与老人的关系
     */
    @TableField("relationship")
    private String relationship;

    /**
     * 是否主要联系人
     * 0表示否，1表示是，默认值为0
     */
    @TableField("is_primary")
    private Integer isPrimary;

    /**
     * 判断是否为主要联系人
     * @return true表示是主要联系人，false表示不是
     */
    public boolean isPrimary() {
        return isPrimary != null && isPrimary == 1;
    }

    /**
     * 设置是否为主要联系人
     * @param primary true表示设为主要联系人，false表示设为非主要联系人
     */
    public void setPrimary(boolean primary) {
        this.isPrimary = primary ? 1 : 0;
    }
}