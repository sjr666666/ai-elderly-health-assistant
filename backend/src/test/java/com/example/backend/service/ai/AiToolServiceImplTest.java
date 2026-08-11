package com.example.backend.service.ai;

import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.GuardianElderRelationMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.MedicineBoxService;
import com.example.backend.service.PlanService;
import com.example.backend.service.SmsNotificationService;
import com.example.backend.service.ai.impl.AiToolServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AiToolServiceImpl 单一真源一致性单测
 * <p>
 * 核心约束（防"改话术漏改一处"）：
 * 1. 工具定义（description）必须包含该工具的全部触发话术（triggers）
 * 2. 系统提示词引导（buildToolGuidance）必须包含全部工具名 + 全部触发话术 + 参数用法
 * 3. 工具定义与引导的数量一致
 * 4. execute 兜底（未知工具 / 缺参数）不抛异常，返回结构化失败 JSON
 */
@ExtendWith(MockitoExtension.class)
class AiToolServiceImplTest {

    @Mock private MedicineBoxService medicineBoxService;
    @Mock private PlanService planService;
    @Mock private SmsNotificationService smsNotificationService;
    @Mock private GuardianElderRelationMapper guardianElderRelationMapper;
    @Mock private UserMapper userMapper;
    @Mock private DrugBaseMapper drugBaseMapper;

    private AiToolServiceImpl toolService;

    @BeforeEach
    void setUp() {
        toolService = new AiToolServiceImpl(medicineBoxService, planService, smsNotificationService,
                guardianElderRelationMapper, userMapper, drugBaseMapper, new ObjectMapper());
    }

    @Test
    void 工具定义_恰好4个且名字正确() {
        List<Map<String, Object>> tools = toolService.getToolDefinitions();
        assertEquals(4, tools.size());
        List<String> names = tools.stream()
                .map(t -> (String) ((Map<?, ?>) t.get("function")).get("name"))
                .toList();
        assertTrue(names.containsAll(List.of(
                "query_medicine_box", "create_medication_plan", "mark_dose_missed", "notify_guardian")));
    }

    @Test
    void description_必须包含各自全部触发话术() {
        List<Map<String, Object>> tools = toolService.getToolDefinitions();
        // 期望：工具名 → 该工具的 triggers（与 TOOL_SPECS 对齐）
        Map<String, List<String>> expected = Map.of(
                "query_medicine_box", List.of("药箱里有什么药", "我都在吃什么药"),
                "create_medication_plan", List.of("帮我安排今天的吃药计划", "今天的药怎么吃"),
                "mark_dose_missed", List.of("今天忘了吃XX药", "XX药没吃"),
                "notify_guardian", List.of("通知", "告诉我家人", "让子女知道"));
        for (Map<String, Object> tool : tools) {
            Map<?, ?> function = (Map<?, ?>) tool.get("function");
            String name = (String) function.get("name");
            String description = (String) function.get("description");
            for (String trigger : expected.get(name)) {
                assertTrue(description.contains(trigger),
                        "工具 " + name + " 的 description 缺少触发话术: " + trigger);
            }
        }
    }

    @Test
    void toolRule_必须包含全部工具名触发话术和参数用法() {
        String guidance = toolService.buildToolGuidance();
        // 全部工具名
        for (String name : List.of("query_medicine_box", "create_medication_plan",
                "mark_dose_missed", "notify_guardian")) {
            assertTrue(guidance.contains(name), "toolRule 缺少工具: " + name);
        }
        // 全部触发话术
        for (String trigger : List.of("药箱里有什么药", "我都在吃什么药", "帮我安排今天的吃药计划",
                "今天的药怎么吃", "今天忘了吃XX药", "XX药没吃", "通知", "告诉我家人", "让子女知道")) {
            assertTrue(guidance.contains(trigger), "toolRule 缺少触发话术: " + trigger);
        }
        // 参数用法说明
        assertTrue(guidance.contains("timeSlot"), "toolRule 缺少 timeSlot 用法说明");
        assertTrue(guidance.contains("message"), "toolRule 缺少 message 用法说明");
        // 引导必须标注工具名（"→ 调用 XX" 格式）
        assertTrue(guidance.contains("→ 调用"), "toolRule 缺少「→ 调用」引导格式");
    }

    @Test
    void 未知工具_返回失败JSON() throws Exception {
        String result = toolService.execute(1L, "not_exist_tool", "{}");
        JsonNode json = new ObjectMapper().readTree(result);
        assertFalse(json.path("success").asBoolean());
        assertTrue(json.path("message").asText().contains("未知工具"));
    }

    @Test
    void markDoseMissed_缺drugName_返回失败JSON() throws Exception {
        String result = toolService.execute(1L, "mark_dose_missed", "{\"timeSlot\":\"morning\"}");
        JsonNode json = new ObjectMapper().readTree(result);
        assertFalse(json.path("success").asBoolean());
        assertTrue(json.path("message").asText().contains("drugName"));
    }
}
