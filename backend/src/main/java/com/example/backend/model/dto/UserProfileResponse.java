package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户档案响应DTO
 * 用于返回用户的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    /**
     * 用户ID（String类型，避免JavaScript精度丢失）
     */
    private String userId;

    /**
     * 真实姓名/称呼
     */
    private String realName;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别：male/female
     */
    private String gender;

    /**
     * 身高（cm）
     */
    private BigDecimal height;

    /**
     * 体重（kg）
     */
    private BigDecimal weight;

    /**
     * 过敏史
     */
    private String allergyHistory;

    /**
     * 慢性病史
     */
    private String chronicDiseases;

    /**
     * 肾功能状态
     */
    private String kidneyFunction;

    /**
     * 肝功能状态
     */
    private String liverFunction;

    /**
     * 是否孕期：0否/1是
     */
    private Integer isPregnant;

    /**
     * 是否哺乳期：0否/1是
     */
    private Integer isBreastfeeding;

    /**
     * 是否吸烟：0否/1是
     */
    private Integer isSmoking;

    /**
     * 是否饮酒：0否/1是
     */
    private Integer isDrinking;

    /**
     * 角色（elder/family）
     */
    private String role;

    /**
     * 联系电话
     */
    private String phone;
}
