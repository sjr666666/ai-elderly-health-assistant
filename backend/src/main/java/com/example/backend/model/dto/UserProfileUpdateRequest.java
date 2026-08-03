package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户档案更新请求DTO
 * 用于更新用户档案信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
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
     * normal/mild_impairment/moderate_impairment/severe_impairment/unknown
     */
    private String kidneyFunction;

    /**
     * 肝功能状态
     * normal/mild_impairment/moderate_impairment/severe_impairment/unknown
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
     * 联系电话
     */
    private String phone;
}
