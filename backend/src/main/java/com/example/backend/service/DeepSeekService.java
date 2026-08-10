package com.example.backend.service;

import com.example.backend.model.dto.DrugConflictRequest;
import com.example.backend.model.dto.DrugConflictResponse;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.DrugSearchResponse;

import java.util.List;
import java.util.Map;

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
     * 使用DeepSeek AI判断药品是处方药还是非处方药
     *
     * @param drugName 药品名称
     * @return "处方药" 或 "非处方药"，判断失败返回null
     */
    String classifyDrugCategory(String drugName);

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

    /**
     * 使用DeepSeek AI回答用户关于药品的追问
     * 基于药品信息和已有对话上下文，回答用户的后续问题
     *
     * @param drugDetail   药品详细信息
     * @param question     用户的追问
     * @param conversationHistory 已有的对话历史（角色: content 格式）
     * @return AI回答文本，失败返回null
     */
    String answerFollowUpQuestion(DrugDetailResponse drugDetail, String question, List<Map<String, String>> conversationHistory);

    /**
     * 通用对话接口：按系统提示 + 用户提示调用大模型，返回回答文本
     * 供 RAG（检索增强生成）等通用场景复用，与现有专用方法共用同一调用链路
     *
     * @param systemPrompt 系统提示（角色设定/约束/引用规则）
     * @param userPrompt   用户提示（检索到的知识 + 用户问题）
     * @return 回答文本；API Key 未配置或调用失败返回 null
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 流式对话接口：SSE 增量回调回答内容（打字机效果，老人等待时有实时反馈）
     * 复用 chat 的 prompt 组装，仅调用方式改为 stream=true 逐块回调
     *
     * @param systemPrompt 系统提示
     * @param userPrompt   用户提示
     * @param onDelta      内容增量回调（可能回调多次；Key 未配置/失败时不回调任何内容）
     */
    void chatStream(String systemPrompt, String userPrompt, java.util.function.Consumer<String> onDelta);
}
