package com.example.backend.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.GuardianElderRelationMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.dto.MedicineBoxResponse;
import com.example.backend.model.dto.TodayPlanResponseDTO;
import com.example.backend.model.dto.TodayPlanItemDTO;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.GuardianElderRelation;
import com.example.backend.model.entity.MedicationPlan;
import com.example.backend.model.entity.SysUser;
import com.example.backend.model.enums.EventType;
import com.example.backend.model.enums.RelationStatus;
import com.example.backend.service.MedicineBoxService;
import com.example.backend.service.PlanService;
import com.example.backend.service.SmsNotificationService;
import com.example.backend.service.ai.AiToolService;
import com.example.backend.service.ai.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 工具调用实现：查药箱 / 建服药计划 / 标记漏服 / 通知家属
 * <p>
 * 所有工具只在本服务内执行，参数来自 LLM 生成的 JSON（已通过 SafetyGuard 注入防护），
 * 返回统一 JSON 字符串由 LLM 归纳成老人听得懂的自然语言。
 */
@Service
public class AiToolServiceImpl implements AiToolService {

    private static final Logger logger = LoggerFactory.getLogger(AiToolServiceImpl.class);

    private final MedicineBoxService medicineBoxService;
    private final PlanService planService;
    private final SmsNotificationService smsNotificationService;
    private final GuardianElderRelationMapper guardianElderRelationMapper;
    private final UserMapper userMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final ObjectMapper objectMapper;

    public AiToolServiceImpl(MedicineBoxService medicineBoxService,
                             PlanService planService,
                             SmsNotificationService smsNotificationService,
                             GuardianElderRelationMapper guardianElderRelationMapper,
                             UserMapper userMapper,
                             DrugBaseMapper drugBaseMapper,
                             ObjectMapper objectMapper) {
        this.medicineBoxService = medicineBoxService;
        this.planService = planService;
        this.smsNotificationService = smsNotificationService;
        this.guardianElderRelationMapper = guardianElderRelationMapper;
        this.userMapper = userMapper;
        this.drugBaseMapper = drugBaseMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 工具规格列表（单一真源）：
     * 加工具 = 加一条 ToolSpec + 一个 execute 分支；改触发话术 = 改 triggers 数组。
     * description（getToolDefinitions）与系统提示词引导（buildToolGuidance）都从这里生成。
     */
    private static final List<ToolSpec> TOOL_SPECS = List.of(
            new ToolSpec(
                    "query_medicine_box",
                    "查询用户家庭药箱里正在服用的药品列表（药名、用量、频率、剩余数量、有效期）",
                    List.of("药箱里有什么药", "我都在吃什么药"),
                    Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                    ""),
            new ToolSpec(
                    "create_medication_plan",
                    "根据用户家庭药箱自动生成今日用药计划（把药箱里的药安排成今天上午/中午/晚上/睡前的服药计划）",
                    List.of("帮我安排今天的吃药计划", "今天的药怎么吃"),
                    Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                    ""),
            new ToolSpec(
                    "mark_dose_missed",
                    "把用户今天某个药品的服药计划标记为「漏服」",
                    List.of("今天忘了吃XX药", "XX药没吃"),
                    markDoseMissedParameters(),
                    "参数 drugName 传药品名；老人说了具体时段就传 timeSlot：morning/noon/evening/before_bed"),
            new ToolSpec(
                    "notify_guardian",
                    "发短信通知用户绑定的家属",
                    List.of("通知", "告诉我家人", "让子女知道"),
                    notifyGuardianParameters(),
                    "参数 message 传要告知的内容"));

    /** mark_dose_missed 参数 JSON Schema */
    private static Map<String, Object> markDoseMissedParameters() {
        Map<String, Object> drugProps = new LinkedHashMap<>();
        drugProps.put("drugName", Map.of("type", "string", "description", "药品名称，通用名/商品名/俗名均可"));
        drugProps.put("timeSlot", Map.of("type", "string", "description",
                "可选，服药时段：morning（上午）/ noon（中午）/ evening（晚上）/ before_bed（睡前）。用户说了具体时段（如「早上忘了吃」）时必填，未说可省略"));
        return Map.of("type", "object", "properties", drugProps, "required", List.of("drugName"));
    }

    /** notify_guardian 参数 JSON Schema */
    private static Map<String, Object> notifyGuardianParameters() {
        Map<String, Object> msgProps = new LinkedHashMap<>();
        msgProps.put("message", Map.of("type", "string", "description", "要通知家属的内容（如「我今天头晕，有点不舒服」）"));
        return Map.of("type", "object", "properties", msgProps, "required", List.of("message"));
    }

    @Override
    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (ToolSpec spec : TOOL_SPECS) {
            // description = summary + 触发话术（单一真源，改 triggers 自动同步）
            String description = spec.getSummary() + "。用户说「" + spec.triggersText() + "」时调用";
            tools.add(tool(spec.getName(), description, spec.getParameters()));
        }
        return tools;
    }

    @Override
    public String buildToolGuidance() {
        StringBuilder guide = new StringBuilder();
        guide.append("\n工具能力：你可以调用系统工具帮老人真正办成事（不是假装答应）：\n");
        int i = 1;
        for (ToolSpec spec : TOOL_SPECS) {
            guide.append(i++).append(". 老人说「").append(spec.triggersText()).append("」 → 调用 ")
                    .append(spec.getName());
            if (!spec.getUsage().isEmpty()) {
                guide.append("（").append(spec.getUsage()).append("）");
            }
            guide.append("\n");
        }
        guide.append("调用工具后，把工具返回的结果用大白话告诉老人（如「您的药箱里有3种药：…」）。\n");
        return guide.toString();
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    @Override
    public String execute(Long userId, String toolName, String argumentsJson) {
        try {
            switch (toolName == null ? "" : toolName) {
                case "query_medicine_box":
                    return queryMedicineBox(userId);
                case "create_medication_plan":
                    return createMedicationPlan(userId);
                case "mark_dose_missed":
                    return markDoseMissed(userId, argumentsJson);
                case "notify_guardian":
                    return notifyGuardian(userId, argumentsJson);
                default:
                    return jsonResult(false, "未知工具: " + toolName);
            }
        } catch (Exception e) {
            logger.error("工具执行失败 - userId: {}, tool: {}, 错误: {}", userId, toolName, e.getMessage(), e);
            return jsonResult(false, "工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 查药箱
     */
    private String queryMedicineBox(Long userId) {
        List<MedicineBoxResponse> box = medicineBoxService.getMedicineBoxList(userId, "active");
        if (box == null || box.isEmpty()) {
            return jsonResult(false, "药箱是空的，没有正在服用的药品");
        }
        List<Map<String, Object>> drugs = new ArrayList<>();
        for (MedicineBoxResponse item : box) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("drugName", item.getDrugName());
            d.put("dosage", item.getDosage());
            d.put("frequency", item.getFrequency());
            d.put("remainingQuantity", item.getRemainingQuantity());
            d.put("expiryDate", item.getExpiryDate());
            drugs.add(d);
        }
        return jsonResult(true, "药箱查询成功", Map.of("count", drugs.size(), "drugs", drugs));
    }

    /**
     * 建服药计划（药箱 → 今日计划）
     * 兜底：药箱药品若从未选过时段（业务上不会自动生成计划），
     * 工具按服用频率自动分配默认时段（幂等，已有计划的时段跳过）
     */
    private String createMedicationPlan(Long userId) {
        List<MedicineBoxResponse> box = medicineBoxService.getMedicineBoxList(userId, "active");
        if (box == null || box.isEmpty()) {
            return jsonResult(false, "药箱是空的，没有可以安排计划的药品");
        }
        for (MedicineBoxResponse item : box) {
            if (item.getBoxItemId() == null) {
                continue;
            }
            List<String> slots = inferTimeSlots(item.getFrequency());
            try {
                planService.addBoxItemToMedicationPlan(userId, item.getBoxItemId(), slots);
            } catch (Exception e) {
                logger.warn("AI 为药品添加计划失败 - boxItemId: {}, 错误: {}", item.getBoxItemId(), e.getMessage());
            }
        }

        TodayPlanResponseDTO plan = planService.generateDailyPlanFromMedicineBox(userId);
        List<TodayPlanItemDTO> items = plan == null ? List.of() : plan.getItems();
        if (items == null || items.isEmpty()) {
            return jsonResult(false, "暂时无法生成今日用药计划，请先在用药日历中为药品选择服用时段");
        }
        List<Map<String, Object>> planItems = items.stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("drugName", item.getDrugName());
            m.put("timeSlotLabel", item.getTimeSlotLabel());
            m.put("dosageAtTime", item.getDosageAtTime());
            m.put("status", item.getStatus());
            return m;
        }).collect(Collectors.toList());
        return jsonResult(true, "今日用药计划已生成", Map.of("count", planItems.size(), "plans", planItems));
    }

    /**
     * 按服用频率推断默认服药时段
     */
    private List<String> inferTimeSlots(String frequency) {
        if (frequency == null || frequency.isEmpty()) {
            return List.of("morning", "evening");
        }
        if (frequency.contains("3") || frequency.contains("三")) {
            return List.of("morning", "noon", "evening");
        }
        if (frequency.contains("2") || frequency.contains("两") || frequency.contains("二")) {
            return List.of("morning", "evening");
        }
        if (frequency.contains("1") || frequency.contains("一") || frequency.contains("睡前")) {
            return List.of("morning");
        }
        return List.of("morning", "evening");
    }

    /**
     * 标记漏服：按药品名（+可选时段）解析今日计划 → 执行 skip
     */
    private String markDoseMissed(Long userId, String argumentsJson) {
        JsonNode args = parseArgs(argumentsJson);
        String drugName = args != null ? args.path("drugName").asText("") : "";
        String timeSlot = args != null ? args.path("timeSlot").asText("") : "";
        if (drugName == null || drugName.trim().isEmpty()) {
            return jsonResult(false, "缺少参数 drugName");
        }

        // 今日计划（排除已服用）
        List<MedicationPlan> plans = planService.list(new LambdaQueryWrapper<MedicationPlan>()
                .eq(MedicationPlan::getUserId, userId)
                .eq(MedicationPlan::getPlanDate, LocalDate.now())
                .ne(MedicationPlan::getStatus, MedicationPlan.Status.TAKEN.getCode()));

        // 批量解析药品名（通用名/商品名/俗名）
        List<MedicationPlan> matched = new ArrayList<>();
        List<String> matchedNames = new ArrayList<>();
        for (MedicationPlan plan : plans) {
            DrugBase drug = plan.getDrugId() != null ? drugBaseMapper.selectById(plan.getDrugId()) : null;
            List<String> names = new ArrayList<>();
            if (drug != null) {
                if (drug.getGenericName() != null) names.add(drug.getGenericName());
                if (drug.getTradeName() != null) names.add(drug.getTradeName());
                if (drug.getCommonName() != null) names.add(drug.getCommonName());
            }
            String name = names.isEmpty() ? "" : names.get(0);
            boolean nameHit = names.stream().anyMatch(n -> n.contains(drugName) || drugName.contains(n));
            if (!nameHit) {
                continue;
            }
            // 用户说了具体时段（如"早上忘了吃"）→ 只匹配该时段
            if (timeSlot != null && !timeSlot.isEmpty() && !timeSlot.equals(plan.getTimeSlot())) {
                continue;
            }
            matched.add(plan);
            matchedNames.add(name);
        }

        if (matched.isEmpty()) {
            String slotText = (timeSlot != null && !timeSlot.isEmpty()) ? "（" + timeSlot + "）" : "";
            return jsonResult(false, "今日没有找到药品「" + drugName + "」" + slotText + "的服药计划，请确认药名是否正确");
        }
        if (matched.size() > 1) {
            List<String> timeLabels = matched.stream()
                    .map(p -> p.getTimeSlot() + "(" + p.getId() + ")")
                    .collect(Collectors.toList());
            return jsonResult(false, "「" + drugName + "」今天有多个服药时段: " + String.join("、", timeLabels)
                    + "，请指定要标记漏服的时段");
        }

        MedicationPlan target = matched.get(0);
        planService.executeMedicationAction(target.getId(), userId, "skip");
        logger.info("AI 标记漏服成功 - userId: {}, planId: {}, drug: {}, timeSlot: {}",
                userId, target.getId(), matchedNames.get(0), target.getTimeSlot());
        return jsonResult(true, "已把「" + matchedNames.get(0) + "」（" + target.getTimeSlot() + "）标记为漏服");
    }

    /**
     * 通知家属（短信）
     */
    private String notifyGuardian(Long userId, String argumentsJson) {
        JsonNode args = parseArgs(argumentsJson);
        String message = args != null ? args.path("message").asText("") : "";
        if (message == null || message.trim().isEmpty()) {
            return jsonResult(false, "缺少参数 message");
        }

        List<GuardianElderRelation> relations = guardianElderRelationMapper.selectList(
                new LambdaQueryWrapper<GuardianElderRelation>()
                        .eq(GuardianElderRelation::getElderId, userId)
                        .eq(GuardianElderRelation::getStatus, RelationStatus.ACTIVE.getCode()));
        if (relations == null || relations.isEmpty()) {
            return jsonResult(false, "当前没有绑定的家属，无法发送通知");
        }

        SysUser elder = userMapper.selectById(userId);
        String elderName = elder != null && elder.getRealName() != null ? elder.getRealName() : "老人";
        String fullMessage = "【" + elderName + "】" + message;
        int notified = 0;
        List<String> guardianNames = new ArrayList<>();
        for (GuardianElderRelation relation : relations) {
            SysUser guardian = userMapper.selectById(relation.getGuardianId());
            String phone = guardian != null ? guardian.getPhone() : "";
            try {
                smsNotificationService.sendNotification(relation.getGuardianId(), userId,
                        EventType.EMERGENCY_ALERT.getCode(), fullMessage, phone);
                notified++;
                if (guardian != null && guardian.getRealName() != null) {
                    guardianNames.add(guardian.getRealName());
                }
            } catch (Exception e) {
                logger.error("AI 通知家属失败 - guardianId: {}", relation.getGuardianId(), e);
            }
        }
        if (notified == 0) {
            return jsonResult(false, "通知家属发送失败，请稍后再试");
        }
        logger.info("AI 通知家属成功 - userId: {}, 通知人数: {}", userId, notified);
        return jsonResult(true, "已通知家属: " + String.join("、", guardianNames) + "，共" + notified + "人",
                Map.of("notified", notified, "guardians", guardianNames));
    }

    private JsonNode parseArgs(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(argumentsJson);
        } catch (Exception e) {
            logger.warn("工具参数 JSON 解析失败: {}", argumentsJson);
            return null;
        }
    }

    private String jsonResult(boolean success, String message) {
        return jsonResult(success, message, null);
    }

    private String jsonResult(boolean success, String message, Object data) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", success);
            result.put("message", message);
            if (data != null) {
                result.put("data", data);
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"结果序列化失败\"}";
        }
    }
}
