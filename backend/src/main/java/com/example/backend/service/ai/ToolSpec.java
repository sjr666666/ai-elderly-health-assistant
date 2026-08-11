package com.example.backend.service.ai;

import java.util.List;
import java.util.Map;

/**
 * AI 工具规格（Function Calling 工具的"单一真源"）
 * <p>
 * 老人触发话术（triggers）、工具说明（summary）、参数定义（parameters）、
 * 参数用法（usage）全部收拢在一条 ToolSpec 里，由 {@link AiToolService} 统一生成：
 * <ul>
 *   <li>{@link AiToolService#getToolDefinitions()}  → 拼 tools 的 description（给模型决策用）</li>
 *   <li>{@link AiToolService#buildToolGuidance()}  → 拼系统提示词的 few-shot 引导（给模型触发用）</li>
 * </ul>
 * <b>维护规则：加工具 = 加一条 ToolSpec + 一个 execute 分支；改话术 = 改 triggers 数组。</b>
 */
public class ToolSpec {

    /** 工具名（execute 分支与之对应） */
    private final String name;

    /** 一句话说明（description 主句） */
    private final String summary;

    /** 老人触发话术（可多条，description 与 toolRule 都从它生成） */
    private final List<String> triggers;

    /** 参数 JSON Schema（OpenAI tools 格式） */
    private final Map<String, Object> parameters;

    /** 参数用法说明（拼进 toolRule，可为空串） */
    private final String usage;

    public ToolSpec(String name, String summary, List<String> triggers,
                    Map<String, Object> parameters, String usage) {
        this.name = name;
        this.summary = summary;
        this.triggers = triggers;
        this.parameters = parameters;
        this.usage = usage == null ? "" : usage;
    }

    public String getName() {
        return name;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getTriggers() {
        return triggers;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String getUsage() {
        return usage;
    }

    /** 触发话术拼接（description 与 toolRule 共用），如 "药箱里有什么药/我都在吃什么药" */
    public String triggersText() {
        return String.join("/", triggers);
    }
}
