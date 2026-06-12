package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：sys_user
 *
 * @author backend
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（雪花算法生成）
     * 用于对外暴露的用户标识
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 登录名
     * 唯一约束，用于用户登录认证
     */
    @TableField("username")
    private String username;

    /**
     * 加密密码
     * 存储经过加密处理的用户密码，不可明文存储
     */
    @TableField("password")
    private String password;

    /**
     * 真实姓名/称呼
     * 用户的实际姓名或常用称呼
     */
    @TableField("real_name")
    private String realName;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 年龄
     * 用户年龄，可选字段
     */
    @TableField("age")
    private Integer age;

    /**
     * 性别：male/female
     */
    @TableField("gender")
    private String gender;

    /**
     * 身高（cm）
     * 用于BMI/剂量计算等用药评估
     */
    @TableField("height")
    private BigDecimal height;

    /**
     * 体重（kg）
     * 用于BMI/剂量计算等用药评估
     */
    @TableField("weight")
    private BigDecimal weight;

    /**
     * 过敏史描述
     * 如"青霉素过敏"，用于用药安全提醒
     */
    @TableField("allergy_history")
    private String allergyHistory;

    /**
     * 慢性病史描述
     * 如"高血压、糖尿病"，用于用药安全评估
     */
    @TableField("chronic_diseases")
    private String chronicDiseases;

    /**
     * 肾功能状态
     * normal/mild_impairment/moderate_impairment/severe_impairment/unknown
     */
    @TableField("kidney_function")
    private String kidneyFunction;

    /**
     * 肝功能状态
     * normal/mild_impairment/moderate_impairment/severe_impairment/unknown
     */
    @TableField("liver_function")
    private String liverFunction;

    /**
     * 是否孕期：0否/1是
     */
    @TableField("is_pregnant")
    private Integer isPregnant;

    /**
     * 是否哺乳期：0否/1是
     */
    @TableField("is_breastfeeding")
    private Integer isBreastfeeding;

    /**
     * 是否吸烟：0否/1是
     */
    @TableField("is_smoking")
    private Integer isSmoking;

    /**
     * 是否饮酒：0否/1是
     */
    @TableField("is_drinking")
    private Integer isDrinking;

    /**
     * 角色
     * elder（老人）/ family（家属）
     * 默认值：elder
     */
    @TableField("role")
    private String role;

    /**
     * 家属绑定的老人ID（自关联）
     * 用于建立家属与老人之间的关联关系
     */
    @TableField("bind_elder_id")
    private Long bindElderId;

    

    /**
     * 用户角色枚举
     */
    public enum Role {
        ELDER("elder", "老人"),
        FAMILY("family", "家属");

        private final String code;
        private final String description;

        Role(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static Role fromCode(String code) {
            for (Role role : values()) {
                if (role.code.equals(code)) {
                    return role;
                }
            }
            return ELDER;
        }
    }

    /**
     * 器官功能状态枚举
     */
    public enum OrganFunction {
        NORMAL("normal", "正常"),
        MILD("mild_impairment", "轻度不全"),
        MODERATE("moderate_impairment", "中度不全"),
        SEVERE("severe_impairment", "重度不全"),
        UNKNOWN("unknown", "不详");

        private final String code;
        private final String description;

        OrganFunction(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static OrganFunction fromCode(String code) {
            if (code == null) {
                return UNKNOWN;
            }
            for (OrganFunction v : values()) {
                if (v.code.equalsIgnoreCase(code)) {
                    return v;
                }
            }
            return UNKNOWN;
        }
    }
}