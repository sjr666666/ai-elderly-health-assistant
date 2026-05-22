package com.example.backend.service.impl;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.entity.AiConversationLog;
import com.example.backend.service.AiConversationLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * AI紧急助手服务实现类
 */
@Service
public class AiEmergencyServiceImpl implements AiEmergencyService {

    private static final Logger logger = LoggerFactory.getLogger(AiEmergencyServiceImpl.class);

    @Value("${deepseek.api-url:https://api.deepseek.com/v1}")
    private String apiUrl;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.timeout:30000}")
    private int timeout;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiConversationLogService conversationLogService;

    // 紧急关键词列表
    private static final Set<String> EMERGENCY_KEYWORDS = Set.of(
            "紧急", "救命", "晕倒", "昏迷", "心跳", "呼吸", "出血", "车祸", "摔倒", "骨折",
            "中风", "心梗", "胸痛", "窒息", "中毒", "溺水", "触电", "烧伤", "烫伤", "休克",
            "抽搐", "发作", "危险", "快", "马上", "立刻", "急救"
    );

    // 问题分类标签
    private static final List<Map<String, String>> CATEGORY_TAGS = Arrays.asList(
            Map.of("id", "cardiac", "name", "❤️ 心脏相关", "icon", "❤️"),
            Map.of("id", "respiratory", "name", "🫁 呼吸问题", "icon", "🫁"),
            Map.of("id", "injury", "name", "🤕 外伤处理", "icon", "🤕"),
            Map.of("id", "stroke", "name", "🧠 中风症状", "icon", "🧠"),
            Map.of("id", "poisoning", "name", "⚠️ 中毒处理", "icon", "⚠️"),
            Map.of("id", "fall", "name", "🦵 摔倒骨折", "icon", "🦵"),
            Map.of("id", "choking", "name", "👄 窒息急救", "icon", "👄"),
            Map.of("id", "medication", "name", "💊 用药咨询", "icon", "💊")
    );

    // 离线应答映射
    private static final Map<String, String> OFFLINE_RESPONSES = new ConcurrentHashMap<>();

    static {
        // 心脏相关
        OFFLINE_RESPONSES.put("心脏", "如果出现胸痛、胸闷、心悸等症状，请立即拨打120。让患者保持安静，半卧位休息，松开衣领。如果患者失去意识且无脉搏，应立即进行心肺复苏。");
        OFFLINE_RESPONSES.put("胸痛", "胸痛可能是严重疾病的信号，请立即拨打120。让患者平躺，保持呼吸通畅，不要随意移动。");
        OFFLINE_RESPONSES.put("心跳", "如果出现心跳过快、过慢或不规律，伴有头晕、胸闷等症状，请立即拨打120。让患者保持安静休息。");

        // 呼吸问题
        OFFLINE_RESPONSES.put("呼吸", "呼吸困难是紧急情况，请立即拨打120。让患者保持舒适姿势，确保呼吸道通畅。");
        OFFLINE_RESPONSES.put("窒息", "如果有人窒息，应立即拨打120。如果患者还能咳嗽，鼓励其用力咳嗽。如果无法咳嗽，立即进行海姆立克急救法。");

        // 外伤处理
        OFFLINE_RESPONSES.put("出血", "如果出血较多，请立即用干净的纱布或毛巾按压伤口止血。如果是动脉出血，应在伤口近心端用止血带或绳子绑扎，但每半小时要松开一次。");
        OFFLINE_RESPONSES.put("烧伤", "轻度烧伤：立即用流动的冷水冲洗15-30分钟，然后用干净的纱布覆盖。严重烧伤：不要自行处理，立即拨打120。");
        OFFLINE_RESPONSES.put("烫伤", "立即用流动的冷水冲洗烫伤部位15-30分钟，降低皮肤温度。不要挑破水泡，用干净纱布覆盖。");

        // 中风症状
        OFFLINE_RESPONSES.put("中风", "请立即拨打120！中风的典型症状：面部歪斜、手臂无力、言语不清。让患者平躺，保持呼吸通畅，不要喂水或食物。");
        OFFLINE_RESPONSES.put("头晕", "如果头晕严重或伴有恶心、呕吐、肢体麻木，请立即拨打120。让患者平躺，保持安静。");

        // 中毒处理
        OFFLINE_RESPONSES.put("中毒", "请立即拨打120和110！不要催吐，保留中毒物品供医生参考。如果是皮肤接触，立即用清水冲洗。");
        OFFLINE_RESPONSES.put("误服", "请立即拨打120！保留误服物品的包装，不要自行催吐。");

        // 摔倒骨折
        OFFLINE_RESPONSES.put("摔倒", "如果怀疑骨折，不要移动患者，立即拨打120。可以用书本、木板等临时固定受伤部位。");
        OFFLINE_RESPONSES.put("骨折", "请立即拨打120！不要移动骨折部位，用夹板或书本固定。");

        // 用药咨询
        OFFLINE_RESPONSES.put("吃药", "请严格按照医嘱或药品说明书服用药物。如果忘记服药，不要双倍剂量。如有疑问，请咨询医生或药师。");
        OFFLINE_RESPONSES.put("药物", "在服用任何药物前，请仔细阅读说明书。如果出现不适反应，请立即停药并就医。");

        // 通用紧急情况
        OFFLINE_RESPONSES.put("紧急", "这是紧急情况，请立即拨打120！保持冷静，按照急救知识进行初步处理。");
        OFFLINE_RESPONSES.put("救命", "请立即拨打120！同时保持患者呼吸道通畅，尽量提供急救帮助。");
    }

    public AiEmergencyServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper,
                                  AiConversationLogService conversationLogService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.conversationLogService = conversationLogService;
    }

    @Override
    public ResponseResult<String> handleEmergencyQuestion(Long userId, String question, boolean isEmergency, List<Map<String, String>> history) {
        if (question == null || question.trim().isEmpty()) {
            return ResponseResult.fail("请输入您的问题");
        }

        String response;
        boolean safetyPassed = true;

        try {
            // 优先尝试调用AI，传递历史对话
            response = callDeepSeekAI(question, isEmergency, history);

            if (response == null || response.isEmpty()) {
                // AI调用失败，使用离线应答
                response = getOfflineResponse(question);
            }
        } catch (Exception e) {
            logger.error("调用AI服务失败，使用离线应答 - 错误: {}", e.getMessage());
            response = getOfflineResponse(question);
        }

        // 保存对话记录
        String queryType = isEmergency ? AiConversationLog.QueryType.EMERGENCY.getCode() 
                                      : AiConversationLog.QueryType.EXPLAIN.getCode();
        conversationLogService.saveLog(userId, queryType, question, response, safetyPassed);

        return ResponseResult.success(response);
    }

    /**
     * 调用DeepSeek AI处理紧急问题
     */
    private String callDeepSeekAI(String question, boolean isEmergency, List<Map<String, String>> history) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置");
            return null;
        }

        try {
            logger.info("开始调用DeepSeek AI处理{}问题...", isEmergency ? "紧急" : "普通");

            // 构建完整的API URL
            String fullApiUrl = apiUrl + "/chat/completions";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", isEmergency ? 0.3 : 0.5);
            requestBody.put("max_tokens", 500);

            String systemPrompt = buildSystemPrompt(isEmergency);

            // 构建消息列表，包含系统提示、历史对话和当前问题
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 1. 系统提示
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // 2. 历史对话
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> hist : history) {
                    Map<String, String> histMessage = new HashMap<>();
                    histMessage.put("role", hist.getOrDefault("role", "user"));
                    histMessage.put("content", hist.getOrDefault("content", ""));
                    messages.add(histMessage);
                }
            }

            // 3. 当前用户问题
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            logger.info("调用DeepSeek API: {}", fullApiUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(fullApiUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                return parseResponse(responseBody);
            } else {
                logger.error("DeepSeek API请求失败 - 状态码: {}, 响应: {}", 
                        response.getStatusCode(), response.getBody());
                return null;
            }

        } catch (RestClientException e) {
            logger.error("DeepSeek API调用异常 - 错误: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(boolean isEmergency) {
        if (isEmergency) {
            return "你是一位经验丰富、善于沟通的老年护理专家。你的任务是为老年人提供紧急情况下的急救指导。\n\n" +
                    "重要原则：\n" +
                    "1. 优先建议立即拨打120急救电话\n" +
                    "2. 只提供简单、明确的2-3个紧急步骤\n" +
                    "3. 使用日常用语，不要用医学术语\n" +
                    "4. 保持语气温和，让用户感到安心\n" +
                    "5. 回答要短小精悍，每段不超过2句话\n" +
                    "6. 适当使用表情符号增加亲切感\n\n" +
                    "格式要求：\n" +
                    "- 使用【紧急提醒】标记需要立即行动的事项\n" +
                    "- 使用emoji表情让内容更易读\n" +
                    "- 每条建议之间空一行\n" +
                    "- 避免长段落，尽量使用列表形式";
        } else {
            return "你是一位和蔼可亲的健康顾问，专门为老年人解答健康问题。\n\n" +
                    "回答原则：\n" +
                    "1. 使用简单易懂的大白话，不说专业术语\n" +
                    "2. 回答要短，一次只说一件事\n" +
                    "3. 适当使用表情符号，让内容更亲切\n" +
                    "4. 如果涉及紧急情况，要立即提醒拨打120\n" +
                    "5. 多用'请'、'您'等礼貌用语\n\n" +
                    "格式要求：\n" +
                    "- 每个要点单独成段\n" +
                    "- 重要信息用【】标注\n" +
                    "- 使用emoji增加可读性\n" +
                    "- 避免复杂句式，一句话说清楚一件事";
        }
    }

    /**
     * 解析DeepSeek API响应
     */
    private String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");

            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");

                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null) {
                        return content.asText().trim();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析DeepSeek响应失败 - 错误: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String getOfflineResponse(String question) {
        String lowerQuestion = question.toLowerCase().trim();

        // 查找匹配的离线应答
        for (Map.Entry<String, String> entry : OFFLINE_RESPONSES.entrySet()) {
            if (lowerQuestion.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 如果是紧急情况但没有匹配的离线应答
        if (isEmergencyQuestion(question)) {
            return "⚠️ 这看起来是紧急情况，请立即拨打120急救电话！\n\n" +
                    "在等待救护车时，请保持患者呼吸道通畅，不要随意移动患者。\n" +
                    "如果患者失去意识但有呼吸，请让其平躺，头部偏向一侧。";
        }

        // 默认应答
        return "很抱歉，目前网络连接不稳定，无法获取专业医疗建议。\n\n" +
                "如果您遇到紧急情况，请立即拨打120急救电话。\n" +
                "如果是非紧急的健康问题，建议您稍后再次尝试或咨询专业医生。";
    }

    @Override
    public boolean isEmergencyQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return false;
        }

        String lowerQuestion = question.toLowerCase();
        for (String keyword : EMERGENCY_KEYWORDS) {
            if (lowerQuestion.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResponseResult<?> getCategoryTags() {
        return ResponseResult.success(CATEGORY_TAGS);
    }
}