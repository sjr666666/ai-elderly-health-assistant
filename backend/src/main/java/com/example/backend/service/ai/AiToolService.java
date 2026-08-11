package com.example.backend.service.ai;

import java.util.List;
import java.util.Map;

/**
 * AI 工具调用服务（Function Calling 工具执行层）
 * <p>
 * 让 LLM 从"只问答"升级为"能行动"：AI 识别用户意图后调用系统工具
 * （查药箱 / 建服药计划 / 标记漏服 / 通知家属），把执行结果带回对话。
 * 工具定义遵循 OpenAI / DeepSeek 兼容的 tools 格式。
 */
public interface AiToolService {

    /**
     * 获取工具定义列表（OpenAI / DeepSeek tools 格式）
     * <pre>
     * [{"type":"function","function":{"name":"...","description":"...","parameters":{...}}}]
     * </pre>
     */
    List<Map<String, Object>> getToolDefinitions();

    /**
     * 执行工具并返回给 LLM 的结果 JSON 字符串
     *
     * @param userId        当前老人用户ID（数据库主键，从 JWT SecurityContext 获取）
     * @param toolName      工具名（query_medicine_box / create_medication_plan / mark_dose_missed / notify_guardian）
     * @param argumentsJson LLM 生成的工具参数 JSON（可为空字符串 "{}"）
     * @return 执行结果 JSON（成功/失败都由 LLM 归纳成自然语言回复给老人）
     */
    String execute(Long userId, String toolName, String argumentsJson);
}
