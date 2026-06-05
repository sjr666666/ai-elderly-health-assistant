package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
