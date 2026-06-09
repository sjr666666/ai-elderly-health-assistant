package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 药品冲突检测请求 DTO
 * 用于批量检测药品之间的相互作用及与其他物质的禁忌搭配
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugConflictRequest {

    /**
     * 要检测的药品名称列表
     */
    private List<String> drugNames;

    /**
     * 用户正在服用的保健品列表（可选）
     */
    private List<String> supplements;

    /**
     * 用户日常饮用的饮品（如酒精、咖啡、茶等）（可选）
     */
    private List<String> beverages;

    /**
     * 用户常吃的食物（可选）
     */
    private List<String> foods;

    /**
     * 用户过敏史（可选）
     * 如"青霉素过敏、海鲜过敏"，用于检测药品与过敏原的冲突
     */
    private String allergyHistory;

    /**
     * 用户慢性病史（可选）
     * 如"高血压、糖尿病、心脏病"，用于检测药品与基础疾病的禁忌
     */
    private String chronicDiseases;

    /**
     * 性别：male/female（可选）
     * 用于妊娠/性别相关用药风险评估
     */
    private String gender;

    /**
     * 年龄（可选）
     * 用于老人/儿童等特殊人群用药风险评估
     */
    private Integer age;

    /**
     * 身高（cm）（可选）
     * 用于BMI/剂量计算等用药评估
     */
    private BigDecimal height;

    /**
     * 体重（kg）（可选）
     * 用于BMI/剂量计算等用药评估
     */
    private BigDecimal weight;

    /**
     * 肾功能状态（可选）
     * normal/mild_impairment/moderate_impairment/severe_impairment/unknown
     */
    private String kidneyFunction;

    /**
     * 肝功能状态（可选）
     * normal/mild_impairment/moderate_impairment/severe_impairment/unknown
     */
    private String liverFunction;

    /**
     * 是否孕期：0否/1是（可选）
     */
    private Integer isPregnant;

    /**
     * 是否哺乳期：0否/1是（可选）
     */
    private Integer isBreastfeeding;

    /**
     * 是否吸烟：0否/1是（可选）
     * 用于吸烟相关用药风险评估
     */
    private Integer isSmoking;

    /**
     * 是否饮酒：0否/1是（可选）
     * 用于酒精相关用药风险评估
     */
    private Integer isDrinking;

    /**
     * 是否需要详细解释
     */
    @Builder.Default
    private boolean detailed = true;

    /**
     * 是否生成替代方案建议
     */
    @Builder.Default
    private boolean includeAlternatives = true;
}
