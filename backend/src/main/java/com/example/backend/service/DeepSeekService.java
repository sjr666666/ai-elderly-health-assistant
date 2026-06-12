package com.example.backend.service;

import com.example.backend.model.dto.DrugConflictRequest;
import com.example.backend.model.dto.DrugConflictResponse;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.DrugSearchResponse;

import java.util.List;

/**
 * DeepSeek AI服务接口
 * 用于调用DeepSeek大语言模型进行药品识别和信息查询
 */
public interface DeepSeekService {

    /**
     * 使用DeepSeek AI分析药品说明书文本，提取药品名称
     *
     * @param ocrText OCR识别的原始文本
     * @return 提取的药品名称，如果未能提取则返回null
     */
    String extractDrugNameWithAI(String ocrText);

    /**
     * 使用DeepSeek AI查询药品详细信息
     *
     * @param drugName 药品名称
     * @return 药品详细信息，如果查询失败则返回null
     */
    DrugDetailResponse queryDrugInfoWithAI(String drugName);

    /**
     * 使用DeepSeek AI生成老年友好版本的用药指导
     * 针对老年人群体的特殊需求进行优化
     *
     * @param drugDetail 药品详细信息
     * @return 老年友好的用药指导文本
     */
    String generateElderlyFriendlyGuide(DrugDetailResponse drugDetail);

    /**
     * 使用DeepSeek AI检测药品之间的冲突
     *
     * @param drugNames 药品名称列表
     * @return 冲突检测结果列表
     */
    DrugConflictResponse checkDrugConflicts(List<String> drugNames);

    /**
     * 使用DeepSeek AI进行全面的药品冲突检测
     * 支持检测药品与食物、饮料、保健品之间的相互作用
     *
     * @param request 冲突检测请求
     * @return 完整的冲突检测报告
     */
    DrugConflictResponse analyzeDrugConflicts(DrugConflictRequest request);

    /**
     * 快速冲突检测（仅使用本地规则，不调用AI）
     * 适合自动检测场景，响应速度快（毫秒级）
     *
     * @param drugNames 药品名称列表
     * @return 冲突检测报告（本地规则检测结果）
     */
    DrugConflictResponse quickCheckWithLocalRules(List<String> drugNames);

    /**
     * 使用DeepSeek AI搜索多个相关药品
     * 根据关键词返回多个匹配的药品信息
     *
     * @param keyword 搜索关键词
     * @return 药品搜索结果列表
     */
    List<DrugSearchResponse> searchMultipleDrugsWithAI(String keyword);

    /**
     * 使用DeepSeek AI生成每日慢病科普文章
     * 基于用户慢性病史和基本信息，生成通俗易懂的健康科普内容
     *
     * @param diseaseName 慢病名称（如"高血压"）
     * @param age 用户年龄（可选）
     * @param gender 用户性别（可选）
     * @return Map包含 "title" 和 "content" 两个key，生成失败返回null
     */
    java.util.Map<String, String> generateDiseaseScienceLesson(String diseaseName, Integer age, String gender);
}
