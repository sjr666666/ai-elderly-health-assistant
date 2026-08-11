package com.example.backend.service.impl;

import com.example.backend.common.util.SafetyGuard;
import com.example.backend.model.dto.DrugConflictRequest;
import com.example.backend.model.dto.DrugConflictResponse;
import com.example.backend.model.dto.DrugConflictResult;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.DrugSearchResponse;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.DeepSeekService;
import com.example.backend.service.rag.RagSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DeepSeek AI服务实现类
 * 调用DeepSeek大语言模型进行药品识别
 */
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekServiceImpl.class);

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    /** RAG 纯检索服务：为药品补全/冲突检测/今日一课注入知识依据，降低幻觉 */
    private final RagSearchService ragSearchService;

    public DeepSeekServiceImpl(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                               ObjectMapper objectMapper,
                               RagSearchService ragSearchService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.ragSearchService = ragSearchService;
    }

    @Override
    public String extractDrugNameWithAI(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            logger.warn("OCR文本为空，无法调用AI识别");
            return null;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，跳过AI识别");
            return null;
        }

        try {
            logger.info("开始调用DeepSeek AI分析药品文本...");

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 50);

            // 构建消息
            String systemPrompt = "你是一个专业的药品识别助手。请分析药品说明书文本，只提取核心的药品名称。" +
                    "不需要规格、生产厂家、适应症、用法用量等信息。直接返回药品名称即可，如果无法识别则返回\"无法识别\"。";

            String userPrompt = "请从以下药品说明书文本中提取药品名称：\n\n" + ocrText + "\n\n药品名称：";

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                String drugName = parseResponse(responseBody);
                
                if (drugName != null && !drugName.isEmpty() && !"无法识别".equals(drugName.trim())) {
                    logger.info("DeepSeek AI识别成功 - 药品名称: {}", drugName);
                    return drugName.trim();
                } else {
                    logger.info("DeepSeek AI未能识别出药品名称");
                    return null;
                }
            } else {
                logger.error("DeepSeek API请求失败 - 状态码: {}, 响应: {}", response.getStatusCode(), response.getBody());
                return null;
            }

        } catch (Exception e) {
            logger.error("调用DeepSeek AI失败 - 错误: {}", e.getMessage(), e);
            return null;
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
    public DrugDetailResponse queryDrugInfoWithAI(String drugName) {
        if (drugName == null || drugName.trim().isEmpty()) {
            logger.warn("药品名称为空，无法调用AI查询");
            return null;
        }

        // 诊断日志：仅记录 API key 是否注入及其长度，不打印任何 key 内容
        logger.info("queryDrugInfoWithAI 入口 - drugName: {}, apiKey是否为空: {}, key长度: {}",
                drugName, apiKey == null || apiKey.isEmpty(),
                apiKey == null ? 0 : apiKey.length());

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，跳过AI查询");
            return null;
        }

        // 清理 key 中可能存在的空白字符
        String cleanKey = apiKey.trim();

        try {
            logger.info("开始调用DeepSeek AI查询药品信息 - 药品名称: {}", drugName);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.2);  // 降低温度让AI回复更简洁
            requestBody.put("max_tokens", 1500);  // 增加token限制以容纳更详细的注意事项

            // 构建消息
            String systemPrompt = "你是一个专业的药品信息查询助手。请提供完整的药品详细信息，以JSON格式输出。\n" +
                    "输出格式必须严格遵循以下JSON结构：\n" +
                    "{\n" +
                    "  \"genericName\": \"药品通用名\",\n" +
                    "  \"tradeName\": \"商品名\",\n" +
                    "  \"specification\": \"规格\",\n" +
                    "  \"manufacturer\": \"生产厂家\",\n" +
                    "  \"category\": \"药品分类\",\n" +
                    "  \"ingredient\": \"药品成分\",\n" +
                    "  \"indications\": \"适应症\",\n" +
                    "  \"usage\": \"用法用量\",\n" +
                    "  \"precautions\": \"注意事项\",\n" +
                    "  \"adverseReactions\": \"不良反应\"\n" +
                    "}\n" +
                    "重要要求：\n" +
                    "0. 如果「用户消息」中提供了参考资料，必须优先使用参考资料中的信息填写（参考资料比模型记忆更可信）；\n" +
                    "1. genericName 必须填写查询的药品名称，不能为空\n" +
                    "2. 对于成分、适应症、用法用量、注意事项、不良反应等字段，优先从参考资料提取，资料中没有的再根据药品知识尽力填写\n" +
                    "3. 如果确实不知道某个字段的信息，可以填写\"尚不明确\"或\"请以药品说明书为准\"\n" +
                    "4. 注意事项应列出具体的用药禁忌和注意事项，不要只写一句话\n" +
                    "5. 不良反应如果未知，填写\"尚不明确\"";

            // RAG 检索增强：先查知识库，把命中资料注入 prompt，避免纯靠模型记忆编造
            String context = ragSearchService.formatContext(drugName, 3);
            String userPrompt = (context.isEmpty() ? "" : context + "\n")
                    + "请查询以下药品的详细信息：" + drugName
                    + "。请优先根据上述参考资料填写成分、适应症、用法用量、注意事项和不良反应，"
                    + "参考资料中没有的字段再根据你的医药知识补充，仍不确定的填\"尚不明确\"。";

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String authHeader = "Bearer " + cleanKey;
            headers.set("Authorization", authHeader);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                DrugDetailResponse drugDetail = parseDrugInfoResponse(responseBody);
                
                if (drugDetail != null) {
                    logger.info("DeepSeek AI查询成功 - 药品: {}, 成分: {}", drugDetail.getGenericName(), drugDetail.getIngredient());
                    return drugDetail;
                } else {
                    logger.info("DeepSeek AI未能查询到药品信息");
                    return null;
                }
            } else {
                logger.error("DeepSeek API请求失败 - 状态码: {}, 响应: {}", response.getStatusCode(), response.getBody());
                return null;
            }

        } catch (Exception e) {
            logger.error("调用DeepSeek AI查询药品信息失败 - 错误: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析DeepSeek药品信息响应
     */
    private DrugDetailResponse parseDrugInfoResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null) {
                        String jsonContent = content.asText().trim();

                        // 记录原始响应，便于排查 AI 返回格式问题
                        logger.info("DeepSeek AI 药品详情原始响应: {}", jsonContent);

                        // 优先尝试解析 JSON
                        try {
                            JsonNode drugJson = objectMapper.readTree(jsonContent);

                            return DrugDetailResponse.builder()
                                    .genericName(getJsonValue(drugJson, "genericName"))
                                    .tradeName(getJsonValue(drugJson, "tradeName"))
                                    .specification(getJsonValue(drugJson, "specification"))
                                    .manufacturer(getJsonValue(drugJson, "manufacturer"))
                                    .category(getJsonValue(drugJson, "category"))
                                    .ingredient(getJsonValue(drugJson, "ingredient"))
                                    .indications(getJsonValue(drugJson, "indications"))
                                    .usage(getJsonValue(drugJson, "usage"))
                                    .precautions(getJsonValue(drugJson, "precautions"))
                                    .adverseReactions(getJsonValue(drugJson, "adverseReactions"))
                                    .description(jsonContent)
                                    .build();
                        } catch (Exception e) {
                            logger.warn("AI 回答非 JSON 格式，尝试从纯文本中提取字段: {}", e.getMessage());
                            return extractDrugInfoFromPlainText(jsonContent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析DeepSeek药品信息响应失败 - 错误: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取JSON节点值，不存在或为空则返回null（不返回兜底文案）
     * 让上层决定如何处理缺失字段
     */
    private String getJsonValue(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName)) {
            String value = node.get(fieldName).asText();
            // 只过滤掉 null 和空字符串，保留"尚不明确"等AI返回的有效信息
            if (value != null && !value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 当 AI 回答为纯文本（非 JSON）时，按关键词逐段提取字段。
     * 解决 "AI 回答了但被硬编码成暂无详细信息" 的问题。
     */
    private DrugDetailResponse extractDrugInfoFromPlainText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String genericName = extractFieldFromText(text, "通用名", "名称");
        String tradeName = extractFieldFromText(text, "商品名", "品牌");
        String specification = extractFieldFromText(text, "规格");
        String manufacturer = extractFieldFromText(text, "生产厂家", "厂家", "生产企业");
        String category = extractFieldFromText(text, "分类", "类别", "类型");
        String ingredient = extractFieldFromText(text, "成分", "主要成分", "有效成分");
        String indications = extractFieldFromText(text, "适应症", "功能主治", "用于", "主治");
        String usage = extractFieldFromText(text, "用法用量", "用法", "用量", "服用方法");
        String precautions = extractFieldFromText(text, "注意事项", "禁忌", "慎用", "禁用");
        String adverseReactions = extractFieldFromText(text, "不良反应", "副作用", "副反应");

        return DrugDetailResponse.builder()
                .genericName(genericName)
                .tradeName(tradeName)
                .specification(specification)
                .manufacturer(manufacturer)
                .category(category)
                .ingredient(ingredient)
                .indications(indications)
                .usage(usage)
                .precautions(precautions)
                .adverseReactions(adverseReactions)
                .description(text)
                .build();
    }

    /**
     * 从纯文本中按关键词提取某个字段的值，文本截至下一个关键词或句末。
     */
    private String extractFieldFromText(String text, String... keywords) {
        for (String keyword : keywords) {
            int idx = text.indexOf(keyword);
            if (idx < 0) continue;
            int start = idx + keyword.length();
            // 跳过冒号/空格
            while (start < text.length()) {
                char c = text.charAt(start);
                if (c == ':' || c == '：' || c == ' ' || c == '\t' || c == '\n' || c == ',') {
                    start++;
                    continue;
                }
                break;
            }
            // 找下一个常见字段作为终止
            String[] stops = {"通用名", "名称", "商品名", "品牌", "规格", "生产厂家", "厂家", "生产企业",
                    "分类", "类别", "类型", "成分", "主要成分", "有效成分", "适应症", "功能主治",
                    "用于", "主治", "用法用量", "用法", "用量", "服用方法", "注意事项", "禁忌",
                    "慎用", "禁用", "不良反应", "副作用", "副反应", "贮藏", "包装", "性状"};
            int end = text.length();
            for (String stop : stops) {
                if (stop.equals(keyword)) continue;
                int next = text.indexOf(stop, start);
                if (next > start && next < end) end = next;
            }
            String value = text.substring(start, Math.min(end, start + 300)).trim();
            // 清理尾部标点
            while (!value.isEmpty() && (value.endsWith("，") || value.endsWith(",") || value.endsWith("：")
                    || value.endsWith(":") || value.endsWith("。") || value.endsWith("；") || value.endsWith(";"))) {
                value = value.substring(0, value.length() - 1).trim();
            }
            if (value.length() >= 2) return value;
        }
        return null; // 提取失败时返回 null，由调用方决定兜底逻辑
    }

    @Override
    public String generateElderlyFriendlyGuide(DrugDetailResponse drugDetail) {
        if (drugDetail == null) {
            logger.warn("药品信息为空，无法生成老年友好用药指导");
            return "对不起，未能获取到药品信息，请稍后再试。";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，使用本地生成老年友好指导");
            return generateLocalElderlyGuide(drugDetail);
        }

        try {
            logger.info("开始调用DeepSeek AI生成老年友好用药指导 - 药品: {}", drugDetail.getGenericName());

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 300); // 限制字数在80-150字左右

            // 构建消息 - 针对老年人群体的特殊需求进行优化
            String systemPrompt = "你是一位慈祥的药剂师，专门为老年人提供用药指导。请用最简单、最亲切的语言，生成一段适合老年人阅读和听力的用药指导。\n\n" +
                    "重要要求：\n" +
                    "1. 语言必须通俗易懂，避免所有专业医学术语\n" +
                    "2. 重点突出三个关键信息：\n" +
                    "   - 【吃多少】每次吃几片，每天吃几次\n" +
                    "   - 【什么时候吃】饭前吃还是饭后吃，早上还是晚上\n" +
                    "   - 【不能做什么】服药后不能做的活动或行为禁忌\n" +
                    "3. 【重要】字数必须控制在80-150字之间，不要太长\n" +
                    "4. 使用第二人称\"您\"，语气亲切温暖\n" +
                    "5. 重要信息前加\"请您注意\"或\"特别提醒\"\n\n" +
                    "输出格式：直接输出一段话，不要使用标题、列表、编号等格式，就像面对面和老人说话一样。";

            String userPrompt = "请为以下药品生成老年友好版本的用药指导：\n\n" +
                    "药品名称：" + (drugDetail.getGenericName() != null ? drugDetail.getGenericName() : "未知") + "\n" +
                    "规格：" + (drugDetail.getSpecification() != null ? drugDetail.getSpecification() : "未知") + "\n" +
                    "用法用量：" + (drugDetail.getUsage() != null ? drugDetail.getUsage() : "未知") + "\n" +
                    "注意事项：" + (drugDetail.getPrecautions() != null ? drugDetail.getPrecautions() : "未知") + "\n" +
                    "不良反应：" + (drugDetail.getAdverseReactions() != null ? drugDetail.getAdverseReactions() : "未知") + "\n\n" +
                    "请用最简单的话告诉老人怎么吃这个药，有什么禁忌。";

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                String guideText = parseResponse(responseBody);
                
                if (guideText != null && !guideText.isEmpty()) {
                    logger.info("DeepSeek AI生成老年友好指导成功");
                    return guideText.trim();
                } else {
                    logger.info("DeepSeek AI未能生成有效指导，使用本地生成");
                    return generateLocalElderlyGuide(drugDetail);
                }
            } else {
                logger.error("DeepSeek API请求失败 - 状态码: {}, 响应: {}", response.getStatusCode(), response.getBody());
                return generateLocalElderlyGuide(drugDetail);
            }

        } catch (Exception e) {
            logger.error("调用DeepSeek AI生成老年友好指导失败 - 错误: {}", e.getMessage(), e);
            return generateLocalElderlyGuide(drugDetail);
        }
    }

    @Override
    public String answerFollowUpQuestion(DrugDetailResponse drugDetail, String question, List<Map<String, String>> conversationHistory) {
        if (drugDetail == null || question == null || question.trim().isEmpty()) {
            return null;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，无法回答追问");
            return null;
        }

        try {
            logger.info("开始调用DeepSeek AI回答追问 - 药品: {}, 问题: {}", drugDetail.getGenericName(), question);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.6);
            requestBody.put("max_tokens", 500);

            // 系统提示：设定AI角色为面向老年人的药剂师
            String systemPrompt = "你是一位慈祥的药剂师，正在为老年人解答用药疑问。\n\n" +
                    "当前药品信息：\n" +
                    "药品名称：" + (drugDetail.getGenericName() != null ? drugDetail.getGenericName() : "未知") + "\n" +
                    "规格：" + (drugDetail.getSpecification() != null ? drugDetail.getSpecification() : "未知") + "\n" +
                    "用法用量：" + (drugDetail.getUsage() != null ? drugDetail.getUsage() : "未知") + "\n" +
                    "注意事项：" + (drugDetail.getPrecautions() != null ? drugDetail.getPrecautions() : "未知") + "\n" +
                    "不良反应：" + (drugDetail.getAdverseReactions() != null ? drugDetail.getAdverseReactions() : "未知") + "\n\n" +
                    "回答要求：\n" +
                    "1. 语言通俗易懂，避免专业术语，像和家里长辈说话一样\n" +
                    "2. 语气温暖亲切，使用\"您\"\n" +
                    "3. 回答要简洁，控制在200字以内\n" +
                    "4. 无论用户的问题是否与当前药品有关，都要认真回答用户的问题；若问题与当前药品相关，优先结合药品信息给出贴近的答复；若问题与药品无关，也直接、自然地回答用户所问的内容，不要拒绝或敷衍\n" +
                    "5. 涉及用药调整的建议，提醒用户咨询医生";

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // 添加历史对话上下文（最多保留最近6轮）
            if (conversationHistory != null) {
                int startIndex = Math.max(0, conversationHistory.size() - 12);
                for (int i = startIndex; i < conversationHistory.size(); i++) {
                    messages.add(conversationHistory.get(i));
                }
            }

            // 添加当前问题
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messages.add(userMessage);

            requestBody.put("messages", messages.toArray());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String answer = parseResponse(response.getBody());
                if (answer != null && !answer.isEmpty()) {
                    logger.info("DeepSeek AI回答追问成功");
                    return answer.trim();
                }
            } else {
                logger.error("DeepSeek API请求失败 - 状态码: {}", response.getStatusCode());
            }
            return null;
        } catch (Exception e) {
            logger.error("调用DeepSeek AI回答追问失败 - 错误: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 本地生成老年友好用药指导（当AI不可用时使用）
     * 使用规则化的方式生成简单易懂的指导文本，控制在80-150字
     */
    private String generateLocalElderlyGuide(DrugDetailResponse drugDetail) {
        StringBuilder guide = new StringBuilder();
        String drugName = drugDetail.getGenericName() != null ? drugDetail.getGenericName() : "这个药品";
        
        // 问候语和药品名称（20字左右）
        guide.append("您好，您查询的药品是").append(drugName).append("。");
        
        // 提取用法用量信息（30-40字）
        String usage = drugDetail.getUsage() != null ? drugDetail.getUsage() : "";
        if (!usage.isEmpty()) {
            guide.append("用法：").append(usage).append("。");
        }
        
        // 服用时间（20字左右）
        if (usage.contains("饭前") || usage.contains("空腹")) {
            guide.append("建议饭前半个小时吃，效果更好。");
        } else if (usage.contains("饭后")) {
            guide.append("建议吃完饭半小时后吃，减少胃刺激。");
        } else if (usage.contains("睡前")) {
            guide.append("建议晚上睡觉前服用。");
        }
        
        // 注意事项和禁忌（30-40字）
        String precautions = drugDetail.getPrecautions() != null ? drugDetail.getPrecautions() : "";
        if (precautions.contains("酒")) {
            guide.append("请您注意：服药期间千万不要喝酒！");
        } else if (precautions.contains("开车")) {
            guide.append("请您注意：吃完药后最好不要开车。");
        }
        
        // 结束语（10字左右）
        guide.append("请严格按照医嘱服用，祝您早日康复！");
        
        return guide.toString().replace("\n", "");
    }

    /**
     * 将专业的不良反应描述转换为通俗语言
     */
    private String parseAdverseReactions(String reactions) {
        if (reactions == null || reactions.isEmpty()) {
            return "轻微的不适";
        }
        
        // 常见不良反应关键词替换
        String result = reactions
            .replace("头晕", "头晕乎乎的")
            .replace("恶心", "想吐")
            .replace("呕吐", "呕吐")
            .replace("腹泻", "拉肚子")
            .replace("便秘", "大便不通畅")
            .replace("皮疹", "身上起疹子")
            .replace("瘙痒", "身上痒")
            .replace("嗜睡", "总想睡觉")
            .replace("乏力", "浑身没力气")
            .replace("胃部不适", "胃不舒服")
            .replace("口干", "嘴巴发干");
        
        // 如果太长，截取前100个字符
        if (result.length() > 100) {
            result = result.substring(0, 100) + "...";
        }
        
        return result;
    }

    @Override
    public DrugConflictResponse checkDrugConflicts(List<String> drugNames) {
        if (drugNames == null || drugNames.isEmpty()) {
            logger.warn("药品名称列表为空，无法进行冲突检测");
            return createEmptyResponse(drugNames, null, null, null);
        }

        DrugConflictRequest request = DrugConflictRequest.builder()
                .drugNames(drugNames)
                .detailed(true)
                .includeAlternatives(true)
                .build();

        return analyzeDrugConflicts(request);
    }

    @Override
    public DrugConflictResponse analyzeDrugConflicts(DrugConflictRequest request) {
        if (request == null || request.getDrugNames() == null || request.getDrugNames().isEmpty()) {
            logger.warn("冲突检测请求为空或药品列表为空");
            return createEmptyResponse(
                request != null ? request.getDrugNames() : null,
                request != null ? request.getSupplements() : null,
                request != null ? request.getBeverages() : null,
                request != null ? request.getFoods() : null
            );
        }

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，使用本地规则进行冲突检测");
            return analyzeWithLocalRules(request);
        }

        try {
            logger.info("开始调用DeepSeek AI进行药品冲突检测 - 药品: {}", request.getDrugNames());

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 4000); // 增加到4000以避免JSON被截断

            // 构建系统提示词
            String systemPrompt = buildConflictSystemPrompt(request.isDetailed(), request.isIncludeAlternatives());

            // RAG 检索增强：把相关药品知识注入 prompt，冲突判断基于资料而非纯模型记忆
            String context = ragSearchService.formatContext(
                    String.join("、", request.getDrugNames()), 3);
            String userPrompt = (context.isEmpty() ? "" : context + "\n")
                    + buildConflictUserPrompt(request);

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(requestBody, headers);

            // 发送请求，设置超时时间
            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, httpRequest, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                DrugConflictResponse result = parseConflictResponse(responseBody, request);
                
                if (result != null) {
                    logger.info("DeepSeek AI冲突检测成功 - 检测到 {} 个冲突", 
                        result.getConflicts() != null ? result.getConflicts().size() : 0);
                    
                    // 在AI检测结果基础上，补充检测药品与酒精的冲突（重要安全提示）
                    List<DrugConflictResult> alcoholConflicts = detectAlcoholConflicts(request.getDrugNames());
                    if (!alcoholConflicts.isEmpty()) {
                        logger.info("补充检测到 {} 个药品-酒精冲突", alcoholConflicts.size());
                        // 将酒精冲突合并到结果中
                        if (result.getConflicts() == null) {
                            result.setConflicts(new ArrayList<>());
                        }
                        for (DrugConflictResult alcoholConflict : alcoholConflicts) {
                            boolean exists = result.getConflicts().stream()
                                .anyMatch(c -> c.getDrugA().equals(alcoholConflict.getDrugA()) 
                                    && c.getDrugB().equals(alcoholConflict.getDrugB()));
                            if (!exists) {
                                result.getConflicts().add(alcoholConflict);
                            }
                        }
                        // 统计信息将在所有冲突（含健康档案）合并并去重后统一更新
                    }

                    // 关键安全检查：基于健康档案的 7 个新维度本地规则必须强制生效
                    // 孕期/哺乳/肾肝/吸烟/年龄/体重/饮食：这些是药学硬禁忌，不应被 AI 漏检
                    result.getConflicts().addAll(collectProfileBasedConflicts(request));

                    // 去重（按 drugA+drugB+conflictType 唯一）
                    result.setConflicts(deduplicateConflicts(result.getConflicts()));
                    result.setHasSevereConflict(result.getConflicts().stream()
                        .anyMatch(c -> c.getSeverity() == DrugConflictResult.SeverityLevel.SEVERE));
                    result.setStatistics(buildStatistics(result.getConflicts()));

                    return result;
                } else {
                    logger.info("DeepSeek AI未能检测到有效冲突信息，使用本地规则检测");
                    return analyzeWithLocalRules(request);
                }
            } else {
                logger.error("DeepSeek API请求失败 - 状态码: {}, 响应: {}", response.getStatusCode(), response.getBody());
                return analyzeWithLocalRules(request);
            }

        } catch (Exception e) {
            logger.error("调用DeepSeek AI进行冲突检测失败 - 错误: {}, 将使用本地规则快速返回", e.getMessage(), e);
            // AI调用失败时，立即使用本地规则返回结果，不阻塞用户
            return analyzeWithLocalRules(request);
        }
    }

    /**
     * 构建冲突检测的系统提示词
     */
    private String buildConflictSystemPrompt(boolean detailed, boolean includeAlternatives) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的临床药剂师，擅长分析药品之间的相互作用和禁忌搭配。\n\n");
        prompt.append("请严格按照以下JSON格式输出检测结果：\n\n");
        prompt.append("{\n");
        prompt.append("  \"conflicts\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"drugA\": \"药品A名称\",\n");
        prompt.append("      \"drugB\": \"药品B名称或其他物质名称\",\n");
        prompt.append("      \"conflictType\": \"DRUG_DRUG|DRUG_FOOD|DRUG_BEVERAGE|DRUG_SUPPLEMENT|DRUG_ALLERGY|DRUG_DISEASE|DRUG_PREGNANCY|DRUG_LACTATION|DRUG_KIDNEY|DRUG_LIVER|DRUG_SMOKING|DRUG_AGE|DRUG_WEIGHT\",\n");
        prompt.append("      \"severity\": \"SEVERE|MODERATE|MILD\",\n");
        prompt.append("      \"conflictMechanism\": \"简短描述冲突原理（不超过50字）\",\n");
        prompt.append("      \"conflictExplanation\": \"通俗易懂的解释（不超过80字）\",\n");
        prompt.append("      \"riskWarning\": \"风险提示（不超过30字）\",\n");
        prompt.append("      \"alternatives\": [\"替代方案1\", \"替代方案2\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"generalAdvice\": \"总体用药建议（不超过50字）\"\n");
        prompt.append("}\n\n");
        prompt.append("冲突严重程度说明：\n");
        prompt.append("- SEVERE（重度）：禁止同时使用，可能导致严重不良反应或危及生命\n");
        prompt.append("- MODERATE（中度）：谨慎使用，可能加重副作用或降低药效\n");
        prompt.append("- MILD（轻度）：可以使用，但需要注意观察身体反应\n\n");
        prompt.append("冲突类型说明：\n");
        prompt.append("- DRUG_DRUG：药品与药品之间的冲突\n");
        prompt.append("- DRUG_FOOD：药品与食物之间的冲突\n");
        prompt.append("- DRUG_BEVERAGE：药品与饮料（如酒精、咖啡、茶等）之间的冲突\n");
        prompt.append("- DRUG_SUPPLEMENT：药品与保健品之间的冲突\n");
        prompt.append("- DRUG_ALLERGY：药品与用户过敏史之间的冲突（如用户对青霉素过敏，检测药品是否含青霉素）\n");
        prompt.append("- DRUG_DISEASE：药品与用户慢性病史之间的冲突（如用户有高血压，检测药品是否禁忌用于高血压患者）\n");
        prompt.append("- DRUG_PREGNANCY：药品与孕期禁忌之间的冲突（如利巴韦林、异维A酸、华法林、四环素、链霉素等孕期禁用药物）\n");
        prompt.append("- DRUG_LACTATION：药品与哺乳期禁忌之间的冲突（哺乳期妇女使用是否安全）\n");
        prompt.append("- DRUG_KIDNEY：药品与肾功能不全之间的冲突（肾功能不全患者是否需调整剂量或禁忌）\n");
        prompt.append("- DRUG_LIVER：药品与肝功能不全之间的冲突（肝功能不全患者是否需调整剂量或禁忌）\n");
        prompt.append("- DRUG_SMOKING：药品与吸烟习惯之间的冲突（如吸烟会诱导 CYP1A2，影响茶碱、氯丙嗪等代谢）\n");
        prompt.append("- DRUG_AGE：药品与年龄/特殊人群之间的冲突（如老人/儿童慎用、8岁以下儿童禁用氟喹诺酮类）\n");
        prompt.append("- DRUG_WEIGHT：药品与体重/剂量之间的冲突（用于提示医生根据体重计算剂量，特别是低体重或肥胖患者）\n\n");

        if (detailed) {
            prompt.append("请提供详细的冲突原理和解释，包括药理机制。\n");
        }

        if (includeAlternatives) {
            prompt.append("请为存在冲突的组合提供合理的替代方案建议。\n");
        }

        prompt.append("如果没有检测到冲突，conflicts数组应为空数组[]。\n");
        prompt.append("输出必须是严格的JSON格式，不能包含任何其他文本。\n");
        prompt.append("重要：所有字段内容必须简洁明了，严格遵守字数限制，避免冗长描述。");

        return prompt.toString();
    }

    /**
     * 构建冲突检测的用户提示词
     */
    private String buildConflictUserPrompt(DrugConflictRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下药品、保健品、饮料和食物之间的相互作用：\n\n");

        prompt.append("【药品列表】\n");
        for (String drug : request.getDrugNames()) {
            prompt.append("- ").append(drug).append("\n");
        }

        if (request.getSupplements() != null && !request.getSupplements().isEmpty()) {
            prompt.append("\n【保健品列表】\n");
            for (String supplement : request.getSupplements()) {
                prompt.append("- ").append(supplement).append("\n");
            }
        }

        if (request.getBeverages() != null && !request.getBeverages().isEmpty()) {
            prompt.append("\n【饮料列表】\n");
            for (String beverage : request.getBeverages()) {
                prompt.append("- ").append(beverage).append("\n");
            }
        }

        if (request.getFoods() != null && !request.getFoods().isEmpty()) {
            prompt.append("\n【食物列表】\n");
            for (String food : request.getFoods()) {
                prompt.append("- ").append(food).append("\n");
            }
        }

        if (request.getAllergyHistory() != null && !request.getAllergyHistory().trim().isEmpty()) {
            prompt.append("\n【用户过敏史】\n");
            prompt.append(request.getAllergyHistory()).append("\n");
        }

        if (request.getChronicDiseases() != null && !request.getChronicDiseases().trim().isEmpty()) {
            prompt.append("\n【用户慢性病史】\n");
            prompt.append(request.getChronicDiseases()).append("\n");
        }

        // 关键用药因素（档案扩面）
        prompt.append("\n【用户关键用药因素】\n");
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            prompt.append("- 性别：").append("male".equalsIgnoreCase(request.getGender()) ? "男" : "female".equalsIgnoreCase(request.getGender()) ? "女" : request.getGender()).append("\n");
        }
        if (request.getAge() != null) {
            prompt.append("- 年龄：").append(request.getAge()).append(" 岁\n");
        }
        if (request.getHeight() != null) {
            prompt.append("- 身高：").append(request.getHeight()).append(" cm\n");
        }
        if (request.getWeight() != null) {
            prompt.append("- 体重：").append(request.getWeight()).append(" kg\n");
        }
        if (request.getKidneyFunction() != null && !request.getKidneyFunction().trim().isEmpty()) {
            prompt.append("- 肾功能：").append(SysUser.OrganFunction.fromCode(request.getKidneyFunction()).getDescription()).append("\n");
        }
        if (request.getLiverFunction() != null && !request.getLiverFunction().trim().isEmpty()) {
            prompt.append("- 肝功能：").append(SysUser.OrganFunction.fromCode(request.getLiverFunction()).getDescription()).append("\n");
        }
        if (Integer.valueOf(1).equals(request.getIsPregnant())) {
            prompt.append("- 孕期：是\n");
        }
        if (Integer.valueOf(1).equals(request.getIsBreastfeeding())) {
            prompt.append("- 哺乳期：是\n");
        }
        if (Integer.valueOf(1).equals(request.getIsSmoking())) {
            prompt.append("- 吸烟：是\n");
        }
        if (Integer.valueOf(1).equals(request.getIsDrinking())) {
            prompt.append("- 饮酒：是\n");
        }

        prompt.append("\n请检测所有可能的组合，包括：\n");
        prompt.append("1. 药品与药品之间的相互作用\n");
        prompt.append("2. 药品与保健品之间的相互作用\n");
        prompt.append("3. 药品与饮料之间的相互作用\n");
        prompt.append("4. 药品与食物之间的相互作用\n");
        prompt.append("5. 药品与用户过敏史之间的冲突（检测药品是否含过敏原或与过敏原相关）\n");
        prompt.append("6. 药品与用户慢性病史之间的冲突（检测药品是否禁忌用于该基础疾病患者）\n");
        prompt.append("7. 药品与孕期/哺乳期之间的冲突（重点检查孕期绝对禁忌药品，如利巴韦林、异维A酸、华法林、四环素、链霉素、喹诺酮类等）\n");
        prompt.append("8. 药品与肝/肾功能不全之间的冲突（评估剂量调整或禁忌）\n");
        prompt.append("9. 药品与吸烟习惯之间的冲突（吸烟对茶碱、氯丙嗪、苯二氮卓等药物代谢的影响）\n");
        prompt.append("10. 药品与年龄/特殊人群之间的冲突（≥65岁老人慎用、≤8岁儿童禁用氟喹诺酮类等）\n");
        prompt.append("11. 药品与体重/剂量之间的冲突（低体重或肥胖患者剂量提示）\n");
        prompt.append("\n请提供详细的冲突分析和专业建议。");

        return prompt.toString();
    }

    /**
     * 解析DeepSeek冲突检测响应
     */
    private DrugConflictResponse parseConflictResponse(String responseBody, DrugConflictRequest request) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null) {
                        String jsonContent = content.asText().trim();
                        
                        try {
                            // 尝试修复可能被截断的JSON
                            String fixedJson = fixTruncatedJson(jsonContent);
                            JsonNode resultJson = objectMapper.readTree(fixedJson);
                            
                            List<DrugConflictResult> conflicts = new ArrayList<>();
                            JsonNode conflictsNode = resultJson.get("conflicts");
                            
                            if (conflictsNode != null && conflictsNode.isArray()) {
                                for (JsonNode conflictNode : conflictsNode) {
                                    DrugConflictResult result = parseConflictItem(conflictNode);
                                    if (result != null) {
                                        conflicts.add(result);
                                    }
                                }
                            }
                            
                            String generalAdvice = resultJson.has("generalAdvice") ? 
                                resultJson.get("generalAdvice").asText() : "请遵医嘱用药";
                            
                            return buildResponse(request, conflicts, generalAdvice);
                        } catch (Exception e) {
                            logger.warn("解析冲突检测JSON失败: {}", e.getMessage());
                            logger.warn("原始JSON内容（前500字符）: {}", jsonContent.substring(0, Math.min(500, jsonContent.length())));
                            // 如果JSON解析失败，尝试从已解析的部分提取冲突信息
                            return parsePartialConflicts(jsonContent, request);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析DeepSeek冲突检测响应失败 - 错误: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 解析单个冲突项
     */
    private DrugConflictResult parseConflictItem(JsonNode node) {
        try {
            String drugA = getJsonString(node, "drugA");
            String drugB = getJsonString(node, "drugB");
            String conflictTypeStr = getJsonString(node, "conflictType");
            String severityStr = getJsonString(node, "severity");
            
            if (drugA == null || drugB == null) {
                return null;
            }
            
            DrugConflictResult.ConflictType conflictType = parseConflictType(conflictTypeStr);
            DrugConflictResult.SeverityLevel severity = DrugConflictResult.SeverityLevel.fromString(severityStr);
            
            List<String> alternatives = new ArrayList<>();
            JsonNode alternativesNode = node.get("alternatives");
            if (alternativesNode != null && alternativesNode.isArray()) {
                for (JsonNode altNode : alternativesNode) {
                    alternatives.add(altNode.asText());
                }
            }
            
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(conflictType)
                    .severity(severity)
                    .conflictMechanism(getJsonString(node, "conflictMechanism"))
                    .conflictExplanation(getJsonString(node, "conflictExplanation"))
                    .riskWarning(getJsonString(node, "riskWarning"))
                    .alternatives(alternatives)
                    .build();
        } catch (Exception e) {
            logger.warn("解析冲突项失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析冲突类型
     */
    private DrugConflictResult.ConflictType parseConflictType(String typeStr) {
        if (typeStr == null) {
            return DrugConflictResult.ConflictType.DRUG_DRUG;
        }
        try {
            return DrugConflictResult.ConflictType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return DrugConflictResult.ConflictType.DRUG_DRUG;
        }
    }

    /**
     * 获取JSON字符串值
     */
    private String getJsonString(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName)) {
            String value = node.get(fieldName).asText();
            return value != null && !value.isEmpty() && !"null".equalsIgnoreCase(value) ? value : null;
        }
        return null;
    }

    /**
     * 构建冲突检测响应
     */
    private DrugConflictResponse buildResponse(DrugConflictRequest request,
                                              List<DrugConflictResult> conflicts,
                                              String generalAdvice) {
        int severeCount = 0;
        int moderateCount = 0;
        int mildCount = 0;

        for (DrugConflictResult conflict : conflicts) {
            switch (conflict.getSeverity()) {
                case SEVERE:
                    severeCount++;
                    break;
                case MODERATE:
                    moderateCount++;
                    break;
                case MILD:
                    mildCount++;
                    break;
            }
        }

        DrugConflictResponse.ConflictStatistics statistics = DrugConflictResponse.ConflictStatistics.builder()
                .totalConflicts(conflicts.size())
                .severeCount(severeCount)
                .moderateCount(moderateCount)
                .mildCount(mildCount)
                .build();

        return DrugConflictResponse.builder()
                .reportId(java.util.UUID.randomUUID().toString())
                .checkTime(java.time.LocalDateTime.now())
                .drugsChecked(request.getDrugNames())
                .supplementsChecked(request.getSupplements())
                .beveragesChecked(request.getBeverages())
                .foodsChecked(request.getFoods())
                .allergyHistory(request.getAllergyHistory())
                .chronicDiseases(request.getChronicDiseases())
                .gender(request.getGender())
                .age(request.getAge())
                .height(request.getHeight())
                .weight(request.getWeight())
                .bmi(calculateBmi(request.getHeight(), request.getWeight()))
                .kidneyFunction(request.getKidneyFunction())
                .liverFunction(request.getLiverFunction())
                .isPregnant(request.getIsPregnant())
                .isBreastfeeding(request.getIsBreastfeeding())
                .isSmoking(request.getIsSmoking())
                .isDrinking(request.getIsDrinking())
                .conflicts(conflicts)
                .hasSevereConflict(severeCount > 0)
                .statistics(statistics)
                .generalAdvice(generalAdvice)
                .complete(true)
                .build();
    }

    /**
     * 计算 BMI（体重 kg / 身高 m²）
     */
    private java.math.BigDecimal calculateBmi(java.math.BigDecimal heightCm, java.math.BigDecimal weightKg) {
        if (heightCm == null || weightKg == null) {
            return null;
        }
        java.math.BigDecimal heightM = heightCm.divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        if (heightM.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return null;
        }
        return weightKg.divide(heightM.multiply(heightM), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 修复可能被截断的JSON字符串
     * @param jsonContent 原始JSON内容
     * @return 修复后的JSON内容
     */
    private String fixTruncatedJson(String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            return jsonContent;
        }
        
        StringBuilder fixed = new StringBuilder(jsonContent.trim());
        
        // 如果JSON不完整，尝试补充闭合括号
        int openBraces = 0;
        int openBrackets = 0;
        boolean inString = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < fixed.length(); i++) {
            char c = fixed.charAt(i);
            
            if (escapeNext) {
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                continue;
            }
            
            if (c == '"' && !escapeNext) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') openBraces++;
                else if (c == '}') openBraces--;
                else if (c == '[') openBrackets++;
                else if (c == ']') openBrackets--;
            }
        }
        
        // 如果在字符串中，补充闭合引号和逗号
        if (inString) {
            fixed.append('"');
            // 检查是否是在值的位置被截断，添加逗号分隔下一个字段
            if (fixed.length() > 1 && fixed.charAt(fixed.length() - 2) != ':') {
                fixed.append(',');
            }
        }
        
        // 补充缺失的闭合括号
        while (openBrackets > 0) {
            fixed.append(']');
            openBrackets--;
        }
        
        while (openBraces > 0) {
            fixed.append('}');
            openBraces--;
        }
        
        logger.info("尝试修复截断的JSON: 原始长度={}, 修复后长度={}", jsonContent.length(), fixed.length());
        return fixed.toString();
    }

    /**
     * 从部分JSON中提取冲突信息（当完整解析失败时）
     * @param jsonContent 可能不完整的JSON内容
     * @param request 请求对象
     * @return 部分解析的响应
     */
    private DrugConflictResponse parsePartialConflicts(String jsonContent, DrugConflictRequest request) {
        logger.info("尝试从部分JSON中提取冲突信息");
        
        List<DrugConflictResult> conflicts = new ArrayList<>();
        
        try {
            // 尝试提取conflicts数组
            int conflictsStart = jsonContent.indexOf("\"conflicts\"");
            if (conflictsStart >= 0) {
                // 找到conflicts数组的开始位置
                int arrayStart = jsonContent.indexOf('[', conflictsStart);
                if (arrayStart >= 0) {
                    // 尝试找到第一个完整的冲突对象
                    int objStart = jsonContent.indexOf('{', arrayStart);
                    int objEnd = jsonContent.lastIndexOf('}', Math.min(arrayStart + 5000, jsonContent.length()));
                    
                    if (objStart >= 0 && objEnd > objStart) {
                        // 尝试逐个解析冲突对象
                        String partialArray = jsonContent.substring(arrayStart, objEnd + 1);
                        
                        // 使用正则表达式提取每个冲突对象
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                            "\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}",
                            java.util.regex.Pattern.DOTALL
                        );
                        java.util.regex.Matcher matcher = pattern.matcher(partialArray);
                        
                        while (matcher.find()) {
                            String conflictJson = matcher.group();
                            try {
                                JsonNode conflictNode = objectMapper.readTree(conflictJson);
                                DrugConflictResult result = parseConflictItem(conflictNode);
                                if (result != null && result.getDrugA() != null && result.getDrugB() != null) {
                                    conflicts.add(result);
                                    logger.info("成功解析部分冲突: {} - {}", result.getDrugA(), result.getDrugB());
                                }
                            } catch (Exception e) {
                                // 忽略单个冲突解析失败
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("部分解析失败: {}", e.getMessage());
        }
        
        if (!conflicts.isEmpty()) {
            logger.info("成功从部分JSON中提取 {} 个冲突", conflicts.size());
            return buildResponse(request, conflicts, "检测到药品冲突，但AI响应不完整，建议咨询医生或药师。");
        }
        
        // 如果无法解析任何冲突，返回空结果
        return createEmptyResponse(
            request.getDrugNames(),
            request.getSupplements(),
            request.getBeverages(),
            request.getFoods()
        );
    }

    /**
     * 创建空的响应
     */
    private DrugConflictResponse createEmptyResponse(List<String> drugs,
                                                     List<String> supplements,
                                                     List<String> beverages,
                                                     List<String> foods) {
        return DrugConflictResponse.builder()
                .reportId(java.util.UUID.randomUUID().toString())
                .checkTime(java.time.LocalDateTime.now())
                .drugsChecked(drugs != null ? drugs : new ArrayList<>())
                .supplementsChecked(supplements != null ? supplements : new ArrayList<>())
                .beveragesChecked(beverages != null ? beverages : new ArrayList<>())
                .foodsChecked(foods != null ? foods : new ArrayList<>())
                .conflicts(new ArrayList<>())
                .hasSevereConflict(false)
                .statistics(DrugConflictResponse.ConflictStatistics.builder()
                        .totalConflicts(0)
                        .severeCount(0)
                        .moderateCount(0)
                        .mildCount(0)
                        .build())
                .generalAdvice("未提供检测数据，无法进行冲突检测")
                .complete(false)
                .build();
    }

    /**
     * 使用本地规则进行冲突检测（当AI不可用时）
     */
    private DrugConflictResponse analyzeWithLocalRules(DrugConflictRequest request) {
        logger.info("使用本地规则进行药品冲突检测");
        
        List<DrugConflictResult> conflicts = new ArrayList<>();
        List<String> drugNames = request.getDrugNames();
        
        // 检测药品-药品冲突
        for (int i = 0; i < drugNames.size(); i++) {
            for (int j = i + 1; j < drugNames.size(); j++) {
                DrugConflictResult conflict = checkDrugDrugConflict(drugNames.get(i), drugNames.get(j));
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }
        
        // 检测药品-酒精冲突（自动检测，不依赖前端传递酒精信息）
        // 用户添加药品后，系统应主动提醒服药期间不能饮酒的禁忌
        for (String drug : drugNames) {
            DrugConflictResult conflict = checkAlcoholConflict(drug);
            if (conflict != null) {
                conflicts.add(conflict);
            }
        }
        
        // 检测药品-保健品冲突
        if (request.getSupplements() != null) {
            for (String supplement : request.getSupplements()) {
                for (String drug : drugNames) {
                    DrugConflictResult conflict = checkDrugSupplementConflict(drug, supplement);
                    if (conflict != null) {
                        conflicts.add(conflict);
                    }
                }
            }
        }
        
        // 检测药品-食物冲突
        if (request.getFoods() != null) {
            for (String food : request.getFoods()) {
                for (String drug : drugNames) {
                    DrugConflictResult conflict = checkDrugFoodConflict(drug, food);
                    if (conflict != null) {
                        conflicts.add(conflict);
                    }
                }
            }
        }
        
        // 检测药品-过敏史冲突
        if (request.getAllergyHistory() != null && !request.getAllergyHistory().trim().isEmpty()) {
            for (String drug : drugNames) {
                DrugConflictResult conflict = checkDrugAllergyConflict(drug, request.getAllergyHistory());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }
        
        // 检测药品-慢性病史冲突
        if (request.getChronicDiseases() != null && !request.getChronicDiseases().trim().isEmpty()) {
            for (String drug : drugNames) {
                DrugConflictResult conflict = checkDrugDiseaseConflict(drug, request.getChronicDiseases());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        // 检测药品-孕期冲突（仅当用户怀孕时触发）
        if (Integer.valueOf(1).equals(request.getIsPregnant())) {
            for (String drug : drugNames) {
                DrugConflictResult conflict = checkDrugPregnancyConflict(drug);
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        // 检测药品-哺乳期冲突
        if (Integer.valueOf(1).equals(request.getIsBreastfeeding())) {
            for (String drug : drugNames) {
                DrugConflictResult conflict = checkDrugLactationConflict(drug);
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        // 检测药品-肾功能不全冲突
        if (isOrganImpaired(request.getKidneyFunction())) {
            for (String drug : drugNames) {
                DrugConflictResult conflict = checkDrugKidneyConflict(drug, request.getKidneyFunction());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        // 检测药品-肝功能不全冲突
        if (isOrganImpaired(request.getLiverFunction())) {
            for (String drug : drugNames) {
                DrugConflictResult conflict = checkDrugLiverConflict(drug, request.getLiverFunction());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        // 检测药品-吸烟习惯冲突
        if (Integer.valueOf(1).equals(request.getIsSmoking())) {
            for (String drug : drugNames) {
                DrugConflictResult conflict = checkDrugSmokingConflict(drug);
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        // 检测药品-年龄/特殊人群冲突
        if (request.getAge() != null) {
            for (String drug : drugNames) {
                DrugConflictResult conflict = checkDrugAgeConflict(drug, request.getAge());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        // 检测药品-体重/剂量冲突（低体重或肥胖提示）
        if (request.getWeight() != null) {
            for (String drug : drugNames) {
                DrugConflictResult conflict = checkDrugWeightConflict(drug, request.getWeight());
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            }
        }

        String generalAdvice = conflicts.isEmpty()
            ? "未检测到明显的药品冲突，但仍建议在医生或药师指导下使用。"
            : "检测到药品冲突，请务必咨询医生或药师后再使用。";
        
        return buildResponse(request, conflicts, generalAdvice);
    }

    /**
     * 检测药品-药品冲突(本地规则)
     */
    private DrugConflictResult checkDrugDrugConflict(String drugA, String drugB) {
        // 空值检查，避免NullPointerException
        if (drugA == null || drugA.trim().isEmpty() || drugB == null || drugB.trim().isEmpty()) {
            logger.warn("药品名称为空，跳过冲突检测 - drugA: {}, drugB: {}", drugA, drugB);
            return null;
        }
            
        String drugALower = drugA.toLowerCase();
        String drugBLower = drugB.toLowerCase();
        
        // 常见药品冲突规则
        // 1. 抗生素类药物与活菌制剂
        if ((drugALower.contains("头孢") || drugALower.contains("青霉素") || drugALower.contains("阿莫西林")) &&
            (drugBLower.contains("益生菌") || drugBLower.contains("双歧杆菌") || drugBLower.contains("乳酸菌"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("抗生素会抑制或杀灭益生菌中的活性菌株，降低益生菌的疗效。")
                    .conflictExplanation("抗生素会杀死益生菌里的好细菌，让益生菌吃了没效果。")
                    .riskWarning("同时服用会降低益生菌的疗效。")
                    .alternatives(java.util.Arrays.asList("建议间隔2-4小时服用", "咨询医生是否需要调整用药方案"))
                    .build();
        }
        
        // 2. 降压药与非甾体抗炎药
        if ((drugALower.contains("降压") || drugALower.contains("沙坦") || drugALower.contains("普利")) &&
            (drugBLower.contains("布洛芬") || drugBLower.contains("阿司匹林") || drugBLower.contains("消炎痛"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("非甾体抗炎药会影响肾脏的前列腺素合成，导致水钠潴留，从而降低降压药的效果。")
                    .conflictExplanation("止痛药可能会让降压药的效果变差，血压降不下来。")
                    .riskWarning("可能导致血压控制不佳。")
                    .alternatives(java.util.Arrays.asList("咨询医生是否需要调整降压药剂量", "考虑使用对血压影响较小的止痛药"))
                    .build();
        }
        
        // 3. 抗凝药与抗血小板药
        if ((drugALower.contains("华法林") || drugALower.contains("利伐沙班") || drugALower.contains("达比加群")) &&
            (drugBLower.contains("阿司匹林") || drugBLower.contains("氯吡格雷") || drugBLower.contains("替格瑞洛"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("抗凝药与抗血小板药联用会显著增加出血风险，可能导致严重的出血事件。")
                    .conflictExplanation("这两种药一起吃会大大增加出血的风险，可能导致流鼻血、牙龈出血甚至内脏出血。")
                    .riskWarning("严重出血风险，禁止自行联用！")
                    .alternatives(java.util.Arrays.asList("必须在医生严密监控下使用", "医生可能会调整剂量或选择单一药物"))
                    .build();
        }
        
        // 4. 他汀类药物与某些抗真菌药
        if ((drugALower.contains("他汀") || drugALower.contains("阿托伐") || drugALower.contains("辛伐")) &&
            (drugBLower.contains("酮康唑") || drugBLower.contains("伊曲康唑"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("某些抗真菌药会抑制他汀类药物的代谢，导致他汀血药浓度显著升高，增加肌肉损伤风险。")
                    .conflictExplanation("这两种药一起吃会让他汀类药物在体内浓度过高，可能导致肌肉疼痛、无力甚至横纹肌溶解。")
                    .riskWarning("严重肌肉损伤风险，禁止联用！")
                    .alternatives(java.util.Arrays.asList("更换抗真菌药物", "暂时停用他汀类药物", "咨询医生调整方案"))
                    .build();
        }
        
        // 5. 红花油（含水杨酸甲酯）与感冒药（含对乙酰氨基酚/阿司匹林）
        // 红花油主要成分是水杨酸甲酯，与阿司匹林类似，与感冒药同用可能增加胃肠道刺激
        if ((drugALower.contains("红花油") || drugALower.contains("水杨酸")) &&
            (drugBLower.contains("感冒") || drugBLower.contains("感冒灵") || drugBLower.contains("感冒颗粒") || 
             drugBLower.contains("对乙酰氨基酚") || drugBLower.contains("扑热息痛") || drugBLower.contains("阿司匹林"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("红花油含有水杨酸甲酯成分，与感冒药中的解热镇痛成分（对乙酰氨基酚或阿司匹林）合用，可能增加胃肠道刺激和出血风险。")
                    .conflictExplanation("红花油和感冒药都含有止痛成分，一起用会刺激胃，可能引起胃痛、胃出血。")
                    .riskWarning("增加胃肠道不适和出血风险，建议分开使用。")
                    .alternatives(java.util.Arrays.asList("建议间隔4-6小时使用", "外用红花油后避免立即口服感冒药", "咨询医生选择合适的用药方案"))
                    .build();
        }
        
        // 6. 感冒药之间的冲突（多种感冒药含相同成分，容易过量）
        if ((drugALower.contains("感冒") || drugALower.contains("感冒灵") || drugALower.contains("感冒颗粒") ||
             drugALower.contains("感冒清热") || drugALower.contains("感冒胶囊")) &&
            (drugBLower.contains("感冒") || drugBLower.contains("感冒灵") || drugBLower.contains("感冒颗粒") ||
             drugBLower.contains("感冒清热") || drugBLower.contains("感冒胶囊"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("多种感冒药通常含有相同的有效成分（如对乙酰氨基酚），同时服用会导致药物过量，增加肝肾损伤风险。")
                    .conflictExplanation("两种感冒药一起吃，里面的止痛成分会加在一起，可能伤肝伤肾。")
                    .riskWarning("可能导致药物过量，增加肝肾负担！")
                    .alternatives(java.util.Arrays.asList("只选择一种感冒药使用", "咨询医生或药师选择合适的感冒药", "避免自行叠加使用多种感冒药"))
                    .build();
        }
        
        // 7. 含阿司匹林成分的药品与其他解热镇痛药
        if ((drugALower.contains("阿司匹林") || drugALower.contains("乙酰水杨酸") || drugALower.contains("拜阿司匹灵")) &&
            (drugBLower.contains("布洛芬") || drugBLower.contains("对乙酰氨基酚") || drugBLower.contains("扑热息痛") ||
             drugBLower.contains("双氯芬酸") || drugBLower.contains("消炎痛") || drugBLower.contains("芬必得") || drugBLower.contains("普生"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("阿司匹林与其他非甾体抗炎药合用会显著增加胃肠道溃疡和出血的风险，两者竞争血浆蛋白结合位点。")
                    .conflictExplanation("这两种止痛药一起吃会严重刺激胃，可能引起胃出血，非常危险。")
                    .riskWarning("严重胃肠道出血风险，禁止同时服用！")
                    .alternatives(java.util.Arrays.asList("只选择一种止痛药", "咨询医生选择合适的止痛方案", "如必须联用需在医生指导下进行"))
                    .build();
        }
        
        // 7.1 布洛芬(芬必得)与阿司匹林
        if ((drugALower.contains("布洛芬") || drugALower.contains("芬必得")) &&
            (drugBLower.contains("阿司匹林") || drugBLower.contains("乙酰水杨酸") || drugBLower.contains("拜阿司匹灵"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("布洛芬会竞争性地抑制阿司匹林对血小板的不可逆乙酰化作用，降低阿司匹林的心血管保护效果，同时增加胃肠道出血风险。")
                    .conflictExplanation("布洛芬和阿司匹林一起吃会让阿司匹林的保护心脏效果变差，还会刺激胃引起出血。")
                    .riskWarning("降低阿司匹林心血管保护效果，增加出血风险！")
                    .alternatives(java.util.Arrays.asList("如需止痛，咨询医生选择对乙酰氨基酚", "如必须使用布洛芬，应在服用阿司匹林前至少30分钟或后8小时服用", "咨询医生调整用药方案"))
                    .build();
        }
        
        // 7.2 布洛芬(芬必得)与华法林 - 双向匹配
        boolean isIbuprofenA = drugALower.contains("布洛芬") || drugALower.contains("芬必得");
        boolean isWarfarinB = drugBLower.contains("华法林") || drugBLower.contains("香豆素") || drugBLower.contains("抗凝");
        boolean isWarfarinA = drugALower.contains("华法林") || drugALower.contains("香豆素") || drugALower.contains("抗凝");
        boolean isIbuprofenB = drugBLower.contains("布洛芬") || drugBLower.contains("芬必得");
        
        if ((isIbuprofenA && isWarfarinB) || (isWarfarinA && isIbuprofenB)) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("布洛芬会抑制血小板聚集，与华法林合用会显著增加出血风险；同时布洛芬可能置换华法林与血浆蛋白的结合，增强抗凝效果。")
                    .conflictExplanation("布洛芬和华法林一起吃会大大增加出血的风险，可能导致流鼻血、牙龈出血、皮下淤青甚至内脏出血。")
                    .riskWarning("严重出血风险，禁止自行联用！")
                    .alternatives(java.util.Arrays.asList("止痛首选对乙酰氨基酚（扑热息痛）", "如必须使用布洛芬需在医生严密监控下", "定期监测凝血功能(INR值)"))
                    .build();
        }
        
        // 8. 止咳药与镇静药/安眠药
        if ((drugALower.contains("止咳") || drugALower.contains("镇咳") || drugALower.contains("右美沙芬") ||
             drugALower.contains("可待因") || drugALower.contains("止咳糖浆")) &&
            (drugBLower.contains("安定") || drugBLower.contains("唑仑") || drugBLower.contains("安眠") ||
             drugBLower.contains("镇静") || drugBLower.contains("氯丙嗪"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("止咳药中的镇咳成分与镇静安眠药合用会增强中枢神经系统的抑制作用，可能导致呼吸抑制。")
                    .conflictExplanation("止咳药和安眠药一起吃会让你睡得太沉，呼吸变慢，可能醒不过来。")
                    .riskWarning("可能导致呼吸抑制、昏迷，禁止同时服用！")
                    .alternatives(java.util.Arrays.asList("避免同时使用", "咨询医生调整用药方案", "选择不含镇静成分的止咳药"))
                    .build();
        }
        
        // 9. 抗过敏药与镇静药/安眠药
        if ((drugALower.contains("抗过敏") || drugALower.contains("扑尔敏") || drugALower.contains("氯雷他定") ||
             drugALower.contains("西替利嗪") || drugALower.contains("过敏")) &&
            (drugBLower.contains("安定") || drugBLower.contains("唑仑") || drugBLower.contains("安眠") ||
             drugBLower.contains("镇静") || drugBLower.contains("酒精"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("抗过敏药中的抗组胺成分与镇静药合用会增强嗜睡效果，影响日常活动和驾驶安全。")
                    .conflictExplanation("抗过敏药和安眠药一起吃会让你特别困，开车或干活时容易出危险。")
                    .riskWarning("可能影响驾驶和日常活动安全。")
                    .alternatives(java.util.Arrays.asList("避免同时使用", "选择嗜睡副作用较小的抗过敏药", "服药期间避免驾驶"))
                    .build();
        }
        
        // 10. 中成药活血化瘀类与抗凝药
        if ((drugALower.contains("活血") || drugALower.contains("化瘀") || drugALower.contains("三七") ||
             drugALower.contains("丹参") || drugALower.contains("红花") || drugALower.contains("血塞通")) &&
            (drugBLower.contains("华法林") || drugBLower.contains("阿司匹林") || drugBLower.contains("抗凝") ||
             drugBLower.contains("利伐沙班") || drugBLower.contains("肝素"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("活血化瘀类中药具有抗血栓作用，与抗凝药合用会显著增加出血风险。")
                    .conflictExplanation("活血中药和抗凝药一起吃会大大增加出血风险，可能引起流鼻血、牙龈出血甚至内脏出血。")
                    .riskWarning("严重出血风险，禁止自行联用！")
                    .alternatives(java.util.Arrays.asList("必须在医生指导下使用", "定期监测凝血功能", "避免自行添加活血类中药"))
                    .build();
        }
        
        // 11. 含麻黄碱的感冒药与降压药
        if ((drugALower.contains("麻黄") || drugALower.contains("伪麻黄碱") || 
             (drugALower.contains("感冒") && (drugALower.contains("麻黄") || drugALower.contains("伪麻")))) &&
            (drugBLower.contains("降压") || drugBLower.contains("沙坦") || drugBLower.contains("普利") ||
             drugBLower.contains("洛尔") || drugBLower.contains("钙通道阻滞"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("麻黄碱具有收缩血管、升高血压的作用，会降低降压药的疗效。")
                    .conflictExplanation("含麻黄碱的感冒药会让血压升高，降压药的效果就变差了。")
                    .riskWarning("可能导致血压控制不佳。")
                    .alternatives(java.util.Arrays.asList("选择不含麻黄碱的感冒药", "咨询医生调整降压药剂量", "密切监测血压变化"))
                    .build();
        }
        
        // 12. 降糖药与其他影响血糖的药物
        if ((drugALower.contains("降糖") || drugALower.contains("二甲双胍") || drugALower.contains("格列") ||
             drugALower.contains("胰岛素") || drugALower.contains("糖尿病")) &&
            (drugBLower.contains("激素") || drugBLower.contains("强的松") || drugBLower.contains("泼尼松") ||
             drugBLower.contains("地塞米松"))) {
            return DrugConflictResult.builder()
                    .drugA(drugA)
                    .drugB(drugB)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DRUG)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("糖皮质激素会升高血糖，降低降糖药的疗效，可能导致血糖控制不佳。")
                    .conflictExplanation("激素类药物会让血糖升高，降糖药的效果就变差了。")
                    .riskWarning("可能导致血糖波动，需密切监测血糖。")
                    .alternatives(java.util.Arrays.asList("咨询医生调整降糖药剂量", "密切监测血糖变化", "尽量缩短激素使用时间"))
                    .build();
        }
        
        return null;
    }

    /**
     * 检测药品-酒精冲突(本地规则)
     */
    private DrugConflictResult checkAlcoholConflict(String drug) {
        // 空值检查
        if (drug == null || drug.trim().isEmpty()) {
            logger.warn("药品名称为空，跳过酒精冲突检测");
            return null;
        }
            
        String drugLower = drug.toLowerCase();
        
        // 头孢类抗生素与酒精的双硫仑样反应
        if (drugLower.contains("头孢") || drugLower.contains("甲硝唑") || drugLower.contains("氯霉素")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("酒精")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_BEVERAGE)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("头孢类药物抑制酒精代谢，导致乙醛在体内大量堆积，引起双硫仑样反应。")
                    .conflictExplanation("吃了这个药再喝酒，会引起面部潮红、头痛、恶心、心跳加快，严重时可能危及生命。")
                    .riskWarning("服用期间及停药后7天内禁止饮酒！")
                    .alternatives(java.util.Arrays.asList("严格戒酒", "选择不含酒精的饮料", "咨询医生是否需要更换药物"))
                    .build();
        }
        
        // 镇静催眠药与酒精
        if (drugLower.contains("安定") || drugLower.contains("唑仑") || drugLower.contains("巴比妥")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("酒精")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_BEVERAGE)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("酒精与镇静催眠药具有协同作用，会增强中枢神经系统的抑制作用。")
                    .conflictExplanation("安眠药和酒精一起用会让你睡得太沉，可能醒不过来，呼吸也会变慢，非常危险。")
                    .riskWarning("可能导致呼吸抑制、昏迷甚至死亡！")
                    .alternatives(java.util.Arrays.asList("绝对禁止饮酒", "睡前只服用医生推荐的药物"))
                    .build();
        }
        
        // 解热镇痛药与酒精（包括常见感冒药，如感冒灵颗粒、感康等，它们含有对乙酰氨基酚成分）
        if (drugLower.contains("布洛芬") || drugLower.contains("阿司匹林") || drugLower.contains("对乙酰氨基酚") ||
            drugLower.contains("扑热息痛") || drugLower.contains("泰诺") || drugLower.contains("芬必得") ||
            drugLower.contains("感冒灵") || drugLower.contains("感康") || drugLower.contains("白加黑") ||
            drugLower.contains("复方氨酚") || drugLower.contains("氨酚烷胺") || drugLower.contains("氨咖") ||
            drugLower.contains("去痛片") || drugLower.contains("止痛") || drugLower.contains("退烧")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("酒精")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_BEVERAGE)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("酒精会加重解热镇痛药对胃黏膜的刺激，增加胃溃疡和出血的风险。")
                    .conflictExplanation("喝酒时吃感冒药或止痛药会刺激胃，容易引起胃痛、胃出血。")
                    .riskWarning("增加胃部不适和出血风险。")
                    .alternatives(java.util.Arrays.asList("服药期间避免饮酒", "饭后服药以减少胃刺激"))
                    .build();
        }
        
        return null;
    }

    /**
     * 检测药品-保健品冲突(本地规则)
     */
    private DrugConflictResult checkDrugSupplementConflict(String drug, String supplement) {
        // 空值检查
        if (drug == null || drug.trim().isEmpty() || supplement == null || supplement.trim().isEmpty()) {
            logger.warn("药品或保健品名称为空，跳过冲突检测");
            return null;
        }
            
        String drugLower = drug.toLowerCase();
        String supplementLower = supplement.toLowerCase();
        
        // 抗凝药与维生素K
        if ((drugLower.contains("华法林") || drugLower.contains("香豆素")) &&
            supplementLower.contains("维生素k")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB(supplement)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_SUPPLEMENT)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("维生素K是凝血因子合成所必需的，会拮抗华法林的抗凝作用。")
                    .conflictExplanation("维生素K会降低抗凝药的效果，可能导致血栓风险增加。")
                    .riskWarning("可能降低抗凝效果，增加血栓风险。")
                    .alternatives(java.util.Arrays.asList("保持稳定的维生素K摄入量", "咨询医生是否需要调整药物剂量"))
                    .build();
        }
        
        // 降压药与钾补充剂
        if ((drugLower.contains("普利") || drugLower.contains("沙坦")) &&
            (supplementLower.contains("钾") || supplementLower.contains("氯化钾"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB(supplement)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_SUPPLEMENT)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("普利类和沙坦类药物会减少钾的排泄，与钾补充剂联用可能导致高钾血症。")
                    .conflictExplanation("这两种一起吃可能导致体内钾含量过高，引起心跳不规律。")
                    .riskWarning("可能导致高钾血症，影响心脏功能。")
                    .alternatives(java.util.Arrays.asList("避免自行补充钾", "定期监测血钾水平", "咨询医生调整用药"))
                    .build();
        }
        
        return null;
    }

    /**
     * 检测药品-食物冲突(本地规则)
     */
    private DrugConflictResult checkDrugFoodConflict(String drug, String food) {
        // 空值检查
        if (drug == null || drug.trim().isEmpty() || food == null || food.trim().isEmpty()) {
            logger.warn("药品或食物名称为空，跳过冲突检测");
            return null;
        }
            
        String drugLower = drug.toLowerCase();
        String foodLower = food.toLowerCase();
        
        // 降压药与高钾食物
        if ((drugLower.contains("普利") || drugLower.contains("沙坦")) &&
            (foodLower.contains("香蕉") || foodLower.contains("橙子") || foodLower.contains("菠菜") || foodLower.contains("土豆"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB(food)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_FOOD)
                    .severity(DrugConflictResult.SeverityLevel.MILD)
                    .conflictMechanism("这类降压药会减少钾的排泄，与高钾食物同食可能增加高钾血症风险。")
                    .conflictExplanation("吃降压药的同时吃太多高钾食物，可能导致体内钾含量偏高。")
                    .riskWarning("注意控制高钾食物的摄入量。")
                    .alternatives(java.util.Arrays.asList("适量食用高钾食物", "咨询医生是否需要调整饮食"))
                    .build();
        }
        
        // 抗酸药与牛奶
        if ((drugLower.contains("氢氧化铝") || drugLower.contains("碳酸钙")) &&
            foodLower.contains("牛奶")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB(food)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_FOOD)
                    .severity(DrugConflictResult.SeverityLevel.MILD)
                    .conflictMechanism("牛奶中的钙会与抗酸药结合，形成不溶性复合物，降低药物吸收。")
                    .conflictExplanation("喝牛奶时吃抗酸药，会影响药物的吸收效果。")
                    .riskWarning("可能降低抗酸药的疗效。")
                    .alternatives(java.util.Arrays.asList("服药后1-2小时再喝牛奶", "咨询医生是否需要调整服药时间"))
                    .build();
        }
        
        // 他汀类药物与葡萄柚
        if ((drugLower.contains("他汀") || drugLower.contains("阿托伐") || drugLower.contains("辛伐")) &&
            (foodLower.contains("葡萄柚") || foodLower.contains("西柚"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB(food)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_FOOD)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("葡萄柚中的成分会抑制他汀类药物的代谢酶，导致血药浓度升高。")
                    .conflictExplanation("吃他汀类药物时吃葡萄柚，会让药物在体内浓度过高，增加副作用风险。")
                    .riskWarning("可能增加肌肉损伤风险。")
                    .alternatives(java.util.Arrays.asList("避免食用葡萄柚", "选择其他水果"))
                    .build();
        }
        
        return null;
    }

    /**
     * 检测药品与酒精的冲突（独立方法，用于AI检测后的补充检测）
     */
    private List<DrugConflictResult> detectAlcoholConflicts(List<String> drugNames) {
        List<DrugConflictResult> conflicts = new ArrayList<>();
        for (String drug : drugNames) {
            DrugConflictResult conflict = checkAlcoholConflict(drug);
            if (conflict != null) {
                conflicts.add(conflict);
            }
        }
        return conflicts;
    }

    /**
     * 构建冲突统计信息
     */
    private DrugConflictResponse.ConflictStatistics buildStatistics(List<DrugConflictResult> conflicts) {
        int severeCount = 0;
        int moderateCount = 0;
        int mildCount = 0;
        
        for (DrugConflictResult conflict : conflicts) {
            switch (conflict.getSeverity()) {
                case SEVERE:
                    severeCount++;
                    break;
                case MODERATE:
                    moderateCount++;
                    break;
                case MILD:
                    mildCount++;
                    break;
            }
        }
        
        return DrugConflictResponse.ConflictStatistics.builder()
                .totalConflicts(conflicts.size())
                .severeCount(severeCount)
                .moderateCount(moderateCount)
                .mildCount(mildCount)
                .build();
    }

    /**
     * 检测药品-过敏史冲突(本地规则)
     */
    private DrugConflictResult checkDrugAllergyConflict(String drug, String allergyHistory) {
        // 空值检查
        if (drug == null || drug.trim().isEmpty() || allergyHistory == null || allergyHistory.trim().isEmpty()) {
            logger.warn("药品或过敏史为空，跳过冲突检测");
            return null;
        }
            
        String drugLower = drug.toLowerCase();
        String allergyLower = allergyHistory.toLowerCase();
        
        if (allergyLower.contains("青霉素") && 
            (drugLower.contains("青霉素") || drugLower.contains("阿莫西林") || drugLower.contains("氨苄西林"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("青霉素过敏史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_ALLERGY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("用户对青霉素类药物过敏，使用该药品可能引发过敏反应，包括皮疹、呼吸困难甚至过敏性休克。")
                    .conflictExplanation("您对青霉素过敏，这个药属于青霉素类，吃了可能会引起过敏反应，非常危险。")
                    .riskWarning("严重过敏风险，禁止使用！")
                    .alternatives(java.util.Arrays.asList("立即停止使用该药品", "咨询医生更换其他类型抗生素", "告知医生您的过敏史"))
                    .build();
        }
        
        if (allergyLower.contains("头孢") && drugLower.contains("头孢")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("头孢类过敏史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_ALLERGY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("用户对头孢类药物过敏，使用该药品可能引发过敏反应，严重时危及生命。")
                    .conflictExplanation("您对头孢过敏，这个药是头孢类，吃了可能会引起过敏反应，非常危险。")
                    .riskWarning("严重过敏风险，禁止使用！")
                    .alternatives(java.util.Arrays.asList("立即停止使用该药品", "咨询医生更换其他类型抗生素", "告知医生您的过敏史"))
                    .build();
        }
        
        if (allergyLower.contains("磺胺") && 
            (drugLower.contains("磺胺") || drugLower.contains("磺胺嘧啶") || drugLower.contains("磺胺甲恶唑"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("磺胺类过敏史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_ALLERGY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("用户对磺胺类药物过敏，使用该药品可能引发过敏反应。")
                    .conflictExplanation("您对磺胺类药物过敏，这个药属于磺胺类，吃了可能会引起过敏反应。")
                    .riskWarning("严重过敏风险，禁止使用！")
                    .alternatives(java.util.Arrays.asList("立即停止使用该药品", "咨询医生更换其他药物", "告知医生您的过敏史"))
                    .build();
        }
        
        if (allergyLower.contains("阿司匹林") && 
            (drugLower.contains("阿司匹林") || drugLower.contains("乙酰水杨酸"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("阿司匹林过敏史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_ALLERGY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("用户对阿司匹林过敏，使用该药品可能引发过敏反应。")
                    .conflictExplanation("您对阿司匹林过敏，这个药含有阿司匹林成分，吃了可能会引起过敏反应。")
                    .riskWarning("严重过敏风险，禁止使用！")
                    .alternatives(java.util.Arrays.asList("立即停止使用该药品", "咨询医生更换其他止痛药", "告知医生您的过敏史"))
                    .build();
        }
        
        if (allergyLower.contains("酒精") && drugLower.contains("乙醇")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("酒精过敏史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_ALLERGY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("用户对酒精过敏，该药品含有乙醇成分，使用可能引发过敏反应。")
                    .conflictExplanation("您对酒精过敏，这个药含有酒精成分，使用可能会引起过敏反应。")
                    .riskWarning("严重过敏风险，禁止使用！")
                    .alternatives(java.util.Arrays.asList("立即停止使用该药品", "咨询医生更换不含酒精的药品", "告知医生您的过敏史"))
                    .build();
        }
        
        return null;
    }

    /**
     * 检测药品-慢性病史冲突（本地规则）
     */
    private DrugConflictResult checkDrugDiseaseConflict(String drug, String chronicDiseases) {
        String drugLower = drug.toLowerCase();
        String diseaseLower = chronicDiseases.toLowerCase();
        
        if (diseaseLower.contains("高血压") && 
            (drugLower.contains("麻黄") || drugLower.contains("伪麻黄碱") || drugLower.contains("肾上腺素"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("高血压病史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DISEASE)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("麻黄碱类药物具有收缩血管、升高血压的作用，高血压患者使用可能导致血压急剧升高，增加心脑血管事件风险。")
                    .conflictExplanation("您有高血压，这个药含有麻黄碱，会让血压升高，非常危险。")
                    .riskWarning("可能导致血压急剧升高，引发心脑血管事件！")
                    .alternatives(java.util.Arrays.asList("立即停止使用该药品", "咨询医生更换不含麻黄碱的药物", "告知医生您的高血压病史"))
                    .build();
        }
        
        if (diseaseLower.contains("糖尿病") && 
            (drugLower.contains("激素") || drugLower.contains("强的松") || drugLower.contains("泼尼松") || drugLower.contains("地塞米松"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("糖尿病病史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DISEASE)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("糖皮质激素会升高血糖，糖尿病患者使用可能导致血糖控制不佳，增加并发症风险。")
                    .conflictExplanation("您有糖尿病，这个药是激素类药物，会让血糖升高，影响血糖控制。")
                    .riskWarning("可能导致血糖波动，增加糖尿病并发症风险。")
                    .alternatives(java.util.Arrays.asList("密切监测血糖变化", "咨询医生调整降糖药剂量", "尽量缩短激素使用时间"))
                    .build();
        }
        
        if (diseaseLower.contains("心脏病") && 
            (drugLower.contains("麻黄") || drugLower.contains("伪麻黄碱") || drugLower.contains("肾上腺素"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("心脏病病史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DISEASE)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("麻黄碱类药物会加快心率、升高血压，心脏病患者使用可能加重心脏负担，诱发心律失常或心肌缺血。")
                    .conflictExplanation("您有心脏病，这个药含有麻黄碱，会加重心脏负担，非常危险。")
                    .riskWarning("可能诱发心律失常或心肌缺血，禁止使用！")
                    .alternatives(java.util.Arrays.asList("立即停止使用该药品", "咨询医生更换安全的药物", "告知医生您的心脏病史"))
                    .build();
        }
        
        if (diseaseLower.contains("肝") && 
            (drugLower.contains("对乙酰氨基酚") || drugLower.contains("扑热息痛"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("肝功能不全病史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DISEASE)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("对乙酰氨基酚主要在肝脏代谢，肝功能不全患者使用可能加重肝脏负担，增加肝损伤风险。")
                    .conflictExplanation("您有肝病，这个药主要在肝脏代谢，可能会加重肝脏负担。")
                    .riskWarning("可能加重肝脏损伤，需谨慎使用。")
                    .alternatives(java.util.Arrays.asList("避免过量使用", "咨询医生选择对肝脏影响较小的药物", "定期检查肝功能"))
                    .build();
        }
        
        if (diseaseLower.contains("肾") && 
            (drugLower.contains("布洛芬") || drugLower.contains("双氯芬酸") || drugLower.contains("消炎痛"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("肾功能不全病史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DISEASE)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("非甾体抗炎药会影响肾脏的前列腺素合成，肾功能不全患者使用可能进一步损害肾功能。")
                    .conflictExplanation("您有肾病，这个药可能会损害肾功能，需要谨慎使用。")
                    .riskWarning("可能加重肾脏损伤，需谨慎使用。")
                    .alternatives(java.util.Arrays.asList("咨询医生选择对肾脏影响较小的药物", "定期检查肾功能", "避免长期使用"))
                    .build();
        }
        
        if (diseaseLower.contains("胃溃疡") &&
            (drugLower.contains("阿司匹林") || drugLower.contains("布洛芬") || drugLower.contains("双氯芬酸"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("胃溃疡病史")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_DISEASE)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("非甾体抗炎药会抑制胃黏膜前列腺素的合成，破坏胃黏膜屏障，胃溃疡患者使用可能导致溃疡加重或出血。")
                    .conflictExplanation("您有胃溃疡，这个药会刺激胃黏膜，可能导致溃疡加重或出血。")
                    .riskWarning("可能导致胃出血或穿孔，禁止使用！")
                    .alternatives(java.util.Arrays.asList("立即停止使用该药品", "咨询医生更换对胃刺激较小的药物", "告知医生您的胃溃疡病史"))
                    .build();
        }

        return null;
    }

    /**
     * 判断器官功能是否处于"不全"状态（轻度/中度/重度）
     */
    private boolean isOrganImpaired(String organFunction) {
        if (organFunction == null) {
            return false;
        }
        String lower = organFunction.toLowerCase();
        return lower.contains("mild_impairment")
                || lower.contains("moderate_impairment")
                || lower.contains("severe_impairment")
                || lower.contains("不全");
    }

    /**
     * 器官功能状态转中文描述
     */
    private String describeImpairment(String organFunction) {
        if (organFunction == null) {
            return "不全";
        }
        String lower = organFunction.toLowerCase();
        if (lower.contains("severe") || lower.contains("重度")) {
            return "重度不全";
        }
        if (lower.contains("moderate") || lower.contains("中度")) {
            return "中度不全";
        }
        if (lower.contains("mild") || lower.contains("轻度")) {
            return "轻度不全";
        }
        return "不全";
    }

    /**
     * 检测药品-孕期冲突
     */
    private DrugConflictResult checkDrugPregnancyConflict(String drug) {
        if (drug == null || drug.trim().isEmpty()) {
            return null;
        }
        String drugLower = drug.toLowerCase();

        // 利巴韦林：孕期绝对禁忌（致畸）
        if (drugLower.contains("利巴韦林")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("孕期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_PREGNANCY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("利巴韦林具有明确的致畸性，孕期使用可导致胎儿严重畸形，属孕期绝对禁忌。")
                    .conflictExplanation("您现在怀孕了，利巴韦林会导致胎儿畸形，绝对不能吃！")
                    .riskWarning("孕期绝对禁忌，可能导致胎儿严重畸形！")
                    .alternatives(java.util.Arrays.asList("立即停药", "咨询医生更换孕期可用药物", "如近期有妊娠计划，服药期间及停药后6个月内严格避孕"))
                    .build();
        }

        // 异维A酸/维A酸：孕期绝对禁忌
        if (drugLower.contains("异维a酸") || drugLower.contains("维a酸") || drugLower.contains("异维甲酸")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("孕期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_PREGNANCY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("维A酸类药物具有强致畸性，孕期使用可导致胎儿中枢神经系统、心血管系统等多发畸形。")
                    .conflictExplanation("您现在怀孕了，维A酸类药物会让宝宝畸形，绝对不能碰！")
                    .riskWarning("孕期绝对禁忌，强致畸性！")
                    .alternatives(java.util.Arrays.asList("立即停药", "咨询皮肤科/产科医生改用孕期安全药物", "服药期间及停药后至少3个月严格避孕"))
                    .build();
        }

        // 四环素类：孕期禁用
        if (drugLower.contains("四环素") || drugLower.contains("多西环素") || drugLower.contains("米诺环素")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("孕期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_PREGNANCY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("四环素类可穿过胎盘屏障，与发育中的骨骼和牙齿中的钙结合，导致胎儿牙齿黄染、骨骼发育异常。")
                    .conflictExplanation("您怀孕了，四环素类药物会让宝宝牙齿变黄、骨头发育不好，绝对不能吃！")
                    .riskWarning("孕期禁用，可能导致胎儿牙齿和骨骼发育异常！")
                    .alternatives(java.util.Arrays.asList("立即停药", "咨询医生改用青霉素类或头孢类抗生素（需先确认无过敏史）"))
                    .build();
        }

        // 喹诺酮类（沙星）：孕期禁用
        if (drugLower.contains("沙星") || drugLower.contains("诺氟沙星") || drugLower.contains("氧氟沙星")
                || drugLower.contains("环丙沙星") || drugLower.contains("左氧氟沙星")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("孕期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_PREGNANCY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("喹诺酮类动物实验显示对幼年动物的软骨有毒性，孕期使用可能影响胎儿软骨发育。")
                    .conflictExplanation("您怀孕了，喹诺酮类（沙星）可能影响宝宝骨骼发育，不能服用！")
                    .riskWarning("孕期禁用，可能影响胎儿软骨发育！")
                    .alternatives(java.util.Arrays.asList("立即停药", "咨询医生改用孕期安全的抗生素"))
                    .build();
        }

        // 氨基糖苷类（链霉素/庆大霉素/阿米卡星）：孕期慎用
        if (drugLower.contains("链霉素") || drugLower.contains("庆大霉素") || drugLower.contains("阿米卡星")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("孕期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_PREGNANCY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("氨基糖苷类可透过胎盘屏障，对胎儿第Ⅷ对脑神经及肾脏有毒性，可能导致先天性耳聋和肾损害。")
                    .conflictExplanation("您怀孕了，这类抗生素可能让宝宝听力受损、伤肾，禁用！")
                    .riskWarning("孕期禁用，可能导致胎儿耳聋和肾损害！")
                    .alternatives(java.util.Arrays.asList("立即停药", "咨询医生改用其他安全的抗生素"))
                    .build();
        }

        // 华法林：孕期（尤其孕早期）致畸
        if (drugLower.contains("华法林")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("孕期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_PREGNANCY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("华法林可透过胎盘屏障，孕早期使用可能导致胎儿华法林综合征（鼻骨发育不良、点状骨骺等），孕中晚期可致胎儿出血、神经系统异常。")
                    .conflictExplanation("您怀孕了，华法林会让宝宝畸形或出血，非常危险！")
                    .riskWarning("孕期禁用，可能致畸或致胎儿出血！")
                    .alternatives(java.util.Arrays.asList("立即停药并就医", "孕期抗凝可改用低分子肝素"))
                    .build();
        }

        // ACEI/ARB（普利/沙坦）：孕期中后期禁用
        if (drugLower.contains("普利") || drugLower.contains("沙坦")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("孕期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_PREGNANCY)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("ACEI/ARB类降压药在孕中晚期使用可导致胎儿肾衰竭、羊水过少、颅骨发育不全等严重不良结局。")
                    .conflictExplanation("您怀孕了，这类降压药会损害宝宝肾脏和发育，不能吃！")
                    .riskWarning("孕中晚期禁用，可能导致胎儿肾衰竭！")
                    .alternatives(java.util.Arrays.asList("立即停药并就医", "孕期可改用甲基多巴、拉贝洛尔等孕期安全降压药"))
                    .build();
        }

        // 甲硝唑：孕早期慎用
        if (drugLower.contains("甲硝唑") && !drugLower.contains("替硝唑")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("孕期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_PREGNANCY)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("甲硝唑动物实验有致畸风险，孕早期（前3个月）一般建议避免使用；孕中晚期在医生评估下可使用。")
                    .conflictExplanation("您怀孕了，甲硝唑在孕早期可能对宝宝有影响，务必先咨询医生！")
                    .riskWarning("孕早期尽量避免使用，必须使用时需医生严格评估。")
                    .alternatives(java.util.Arrays.asList("咨询医生评估是否必须使用", "可用克林霉素等更安全的替代药"))
                    .build();
        }

        return null;
    }

    /**
     * 检测药品-哺乳期冲突
     */
    private DrugConflictResult checkDrugLactationConflict(String drug) {
        if (drug == null || drug.trim().isEmpty()) {
            return null;
        }
        String drugLower = drug.toLowerCase();

        // 哺乳期禁用：四环素类、喹诺酮类、氯霉素、磺胺类、放射性药物、口服抗凝
        if (drugLower.contains("氯霉素")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("哺乳期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_LACTATION)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("氯霉素可分泌入乳汁，导致婴儿灰婴综合征（循环衰竭、皮肤灰紫），并抑制骨髓。")
                    .conflictExplanation("您正在哺乳，氯霉素会让宝宝出现危险的\"灰婴综合征\"，必须停药！")
                    .riskWarning("哺乳期禁用，可能导致婴儿灰婴综合征！")
                    .alternatives(java.util.Arrays.asList("立即停药", "咨询医生改用安全的抗生素", "用药期间暂停哺乳并定期吸奶维持泌乳"))
                    .build();
        }

        if (drugLower.contains("四环素") || drugLower.contains("多西环素") || drugLower.contains("米诺环素")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("哺乳期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_LACTATION)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("四环素类可分泌入乳汁，与乳汁中钙结合影响婴儿骨骼和牙齿发育。")
                    .conflictExplanation("您正在哺乳，四环素类药物会让宝宝牙齿骨骼受影响，谨慎使用！")
                    .riskWarning("哺乳期慎用，短期小剂量可在医生指导下使用。")
                    .alternatives(java.util.Arrays.asList("咨询医生改用青霉素类或头孢类"))
                    .build();
        }

        if (drugLower.contains("沙星") || drugLower.contains("诺氟沙星") || drugLower.contains("氧氟沙星")
                || drugLower.contains("环丙沙星") || drugLower.contains("左氧氟沙星")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("哺乳期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_LACTATION)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("喹诺酮类可分泌入乳汁，动物实验对幼年动物软骨有毒性，理论上可能影响婴儿关节发育。")
                    .conflictExplanation("您正在哺乳，沙星类药物可能影响宝宝关节发育，谨慎使用！")
                    .riskWarning("哺乳期慎用，建议改用其他抗生素。")
                    .alternatives(java.util.Arrays.asList("咨询医生改用青霉素类或头孢类"))
                    .build();
        }

        if (drugLower.contains("磺胺")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("哺乳期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_LACTATION)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("磺胺类可分泌入乳汁，导致早产儿、新生儿发生核黄疸及溶血性贫血。")
                    .conflictExplanation("您正在哺乳，磺胺类药物可能让宝宝发生黄疸和贫血，谨慎使用！")
                    .riskWarning("哺乳期（尤其早产儿）慎用。")
                    .alternatives(java.util.Arrays.asList("咨询医生改用更安全的抗生素", "用药期间暂停哺乳"))
                    .build();
        }

        // 抗甲状腺药（他巴唑/丙硫氧嘧啶）哺乳期慎用
        if (drugLower.contains("甲巯咪唑") || drugLower.contains("他巴唑")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("哺乳期")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_LACTATION)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("抗甲状腺药可分泌入乳汁，影响婴儿甲状腺功能。")
                    .conflictExplanation("您正在哺乳，这类抗甲状腺药可能影响宝宝甲状腺功能，谨慎使用！")
                    .riskWarning("哺乳期使用需医生评估，监测婴儿甲状腺功能。")
                    .alternatives(java.util.Arrays.asList("如必须使用，丙硫氧嘧啶相对更安全", "定期检查婴儿甲状腺功能"))
                    .build();
        }

        return null;
    }

    /**
     * 检测药品-肾功能不全冲突
     */
    private DrugConflictResult checkDrugKidneyConflict(String drug, String kidneyFunction) {
        if (drug == null || drug.trim().isEmpty()) {
            return null;
        }
        String drugLower = drug.toLowerCase();
        String desc = describeImpairment(kidneyFunction);
        boolean severe = kidneyFunction != null && kidneyFunction.toLowerCase().contains("severe");

        // 氨基糖苷类抗生素：肾毒性
        if (drugLower.contains("庆大霉素") || drugLower.contains("阿米卡星") || drugLower.contains("链霉素")
                || drugLower.contains("卡那霉素") || drugLower.contains("妥布霉素")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("肾功能" + desc)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_KIDNEY)
                    .severity(severe ? DrugConflictResult.SeverityLevel.SEVERE : DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("氨基糖苷类抗生素具有明显肾毒性，肾功能不全患者使用会进一步损害肾功能，并因排泄减少导致药物蓄积中毒。")
                    .conflictExplanation("您肾功能不好，这类抗生素会进一步损害您的肾脏，需要谨慎使用！")
                    .riskWarning(severe ? "重度肾功能不全禁用！" : "肾功能不全需减量或延长给药间隔，监测肾功能。")
                    .alternatives(java.util.Arrays.asList("咨询医生改用其他肾毒性较小的抗生素", "必要时进行血药浓度监测"))
                    .build();
        }

        // 非甾体抗炎药：影响肾灌注
        if (drugLower.contains("布洛芬") || drugLower.contains("双氯芬酸") || drugLower.contains("消炎痛")
                || drugLower.contains("尼美舒利") || drugLower.contains("塞来昔布") || drugLower.contains("美洛昔康")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("肾功能" + desc)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_KIDNEY)
                    .severity(severe ? DrugConflictResult.SeverityLevel.SEVERE : DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("非甾体抗炎药通过抑制前列腺素合成影响肾脏血流灌注，肾功能不全患者使用可能诱发急性肾损伤或加重肾损害。")
                    .conflictExplanation("您肾功能不好，这类止痛药会减少肾脏血流，可能让肾病更严重！")
                    .riskWarning(severe ? "重度肾功能不全禁用！" : "肾功能不全慎用，避免长期使用。")
                    .alternatives(java.util.Arrays.asList("咨询医生改用对乙酰氨基酚", "避免长期使用，密切监测肾功能"))
                    .build();
        }

        // ACEI/ARB：肾功能不全高钾血症风险
        if (drugLower.contains("普利") || drugLower.contains("沙坦")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("肾功能" + desc)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_KIDNEY)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("ACEI/ARB类药物减少钾排泄并可能降低肾小球滤过率，肾功能不全患者使用有高钾血症和肾功能恶化的风险。")
                    .conflictExplanation("您肾功能不好，这类降压药会让血钾升高、肾功能恶化，谨慎使用！")
                    .riskWarning("需在医生指导下使用，定期监测肾功能和血钾。")
                    .alternatives(java.util.Arrays.asList("咨询医生评估是否需要调整剂量", "定期监测血钾、肌酐"))
                    .build();
        }

        // 二甲双胍：肾功能不全禁用/慎用
        if (drugLower.contains("二甲双胍")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("肾功能" + desc)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_KIDNEY)
                    .severity(severe ? DrugConflictResult.SeverityLevel.SEVERE : DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("二甲双胍主要经肾脏排泄，肾功能不全患者使用易发生药物蓄积，增加乳酸酸中毒风险。")
                    .conflictExplanation("您肾功能不好，二甲双胍会在体内堆积，可能引起危险的乳酸酸中毒！")
                    .riskWarning(severe ? "重度肾功能不全禁用！" : "需根据肌酐清除率减量使用。")
                    .alternatives(java.util.Arrays.asList("咨询医生评估是否换用其他降糖药", "提供近期肾功能检查结果给医生参考"))
                    .build();
        }

        return null;
    }

    /**
     * 检测药品-肝功能不全冲突
     */
    private DrugConflictResult checkDrugLiverConflict(String drug, String liverFunction) {
        if (drug == null || drug.trim().isEmpty()) {
            return null;
        }
        String drugLower = drug.toLowerCase();
        String desc = describeImpairment(liverFunction);
        boolean severe = liverFunction != null && liverFunction.toLowerCase().contains("severe");

        // 对乙酰氨基酚（扑热息痛/泰诺/感冒灵等复方感冒药）：肝毒性
        if (drugLower.contains("对乙酰氨基酚") || drugLower.contains("扑热息痛") || drugLower.contains("泰诺")
                || drugLower.contains("感冒灵") || drugLower.contains("感康") || drugLower.contains("白加黑")
                || drugLower.contains("复方氨酚") || drugLower.contains("氨酚烷胺") || drugLower.contains("氨咖黄敏")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("肝功能" + desc)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_LIVER)
                    .severity(severe ? DrugConflictResult.SeverityLevel.SEVERE : DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("对乙酰氨基酚主要在肝脏代谢，经CYP450酶系产生有毒中间代谢产物（NAPQI），由谷胱甘肽解毒。肝功能不全时解毒能力下降，易发生肝损伤。")
                    .conflictExplanation("您肝功能不好，含有对乙酰氨基酚的药物会加重肝脏损伤，谨慎使用！")
                    .riskWarning(severe ? "重度肝功能不全禁用！" : "肝功能不全应避免大剂量或长期使用，监测肝功能。")
                    .alternatives(java.util.Arrays.asList("单次剂量不超过300mg，24小时内不超过2g", "咨询医生选择对肝脏影响较小的解热镇痛药", "定期检查肝功能"))
                    .build();
        }

        // 他汀类调脂药：肝功能异常
        if (drugLower.contains("他汀") || drugLower.contains("阿托伐") || drugLower.contains("辛伐")
                || drugLower.contains("瑞舒伐") || drugLower.contains("普伐") || drugLower.contains("氟伐")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("肝功能" + desc)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_LIVER)
                    .severity(severe ? DrugConflictResult.SeverityLevel.SEVERE : DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("他汀类主要经肝脏代谢，肝功能不全患者使用有肝酶升高、横纹肌溶解风险。")
                    .conflictExplanation("您肝功能不好，他汀类降脂药可能伤肝，需要谨慎！")
                    .riskWarning(severe ? "活动性肝病禁用！" : "肝功能不全应减量，监测肝酶和肌酶。")
                    .alternatives(java.util.Arrays.asList("咨询医生评估是否需要调整剂量", "定期监测肝功能、肌酸激酶"))
                    .build();
        }

        // 抗结核药（异烟肼/利福平/吡嗪酰胺）：肝毒性
        if (drugLower.contains("异烟肼") || drugLower.contains("利福平") || drugLower.contains("吡嗪酰胺")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("肝功能" + desc)
                    .conflictType(DrugConflictResult.ConflictType.DRUG_LIVER)
                    .severity(severe ? DrugConflictResult.SeverityLevel.SEVERE : DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("一线抗结核药均具有不同程度的肝毒性，肝功能不全患者使用会进一步加重肝损伤。")
                    .conflictExplanation("您肝功能不好，抗结核药伤肝，需要严密监测！")
                    .riskWarning(severe ? "重度肝功能不全禁用！" : "需在医生指导下使用，定期监测肝功能。")
                    .alternatives(java.util.Arrays.asList("咨询医生评估是否需要调整方案", "每2-4周监测肝功能"))
                    .build();
        }

        return null;
    }

    /**
     * 检测药品-吸烟习惯冲突
     */
    private DrugConflictResult checkDrugSmokingConflict(String drug) {
        if (drug == null || drug.trim().isEmpty()) {
            return null;
        }
        String drugLower = drug.toLowerCase();

        // 吸烟可诱导 CYP1A2，加快以下药物代谢，降低疗效
        if (drugLower.contains("茶碱") || drugLower.contains("氨茶碱")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("吸烟习惯")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_SMOKING)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("吸烟可诱导肝药酶CYP1A2活性，加快茶碱代谢，降低茶碱血药浓度和疗效；戒烟后则可能导致茶碱浓度上升而中毒，需及时调整剂量。")
                    .conflictExplanation("您吸烟，会加快茶碱在体内的清除，让药效变差；戒烟时也要注意调整剂量。")
                    .riskWarning("吸烟会降低茶碱疗效，戒烟后可能出现茶碱过量。")
                    .alternatives(java.util.Arrays.asList("告知医生您的吸烟史，由医生调整剂量", "戒烟是最好的选择", "戒烟后及时复查血药浓度"))
                    .build();
        }

        if (drugLower.contains("氯丙嗪") || drugLower.contains("奋乃静") || drugLower.contains("氟哌啶醇")
                || drugLower.contains("利培酮") || drugLower.contains("奥氮平")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("吸烟习惯")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_SMOKING)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("吸烟诱导CYP1A2代谢多种抗精神病药物（如氯丙嗪、奥氮平、氟哌啶醇等），降低血药浓度和疗效，戒烟后可能出现剂量过大。")
                    .conflictExplanation("您吸烟，会加快抗精神病药代谢，让药效变差；如戒烟需及时调整剂量。")
                    .riskWarning("吸烟会影响抗精神病药疗效，戒烟后需重新评估。")
                    .alternatives(java.util.Arrays.asList("戒烟是最好的选择", "戒烟时及时复诊调整药物剂量"))
                    .build();
        }

        if (drugLower.contains("安定") || drugLower.contains("地西泮") || drugLower.contains("唑仑")
                || drugLower.contains("苯二氮") || drugLower.contains("阿普唑仑")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("吸烟习惯")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_SMOKING)
                    .severity(DrugConflictResult.SeverityLevel.MILD)
                    .conflictMechanism("吸烟可轻度诱导苯二氮卓类药物代谢，降低其镇静作用；与吸烟的呼吸系统影响叠加，吸烟者使用镇静药时呼吸抑制风险增加。")
                    .conflictExplanation("您吸烟，会轻度降低安眠药效果；同时吸烟对呼吸系统有害，请尽量戒烟。")
                    .riskWarning("吸烟可能影响镇静药效果，戒烟可获益更大。")
                    .alternatives(java.util.Arrays.asList("戒烟并复诊评估", "如出现过度嗜睡需及时就医"))
                    .build();
        }

        // 口服避孕药：吸烟增加血栓风险
        if (drugLower.contains("避孕") || drugLower.contains("雌激素") || drugLower.contains("炔雌醇")) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("吸烟习惯")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_SMOKING)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("吸烟（尤其≥35岁）合并使用含雌激素的口服避孕药会显著增加心肌梗死、脑卒中等血栓栓塞风险。")
                    .conflictExplanation("您吸烟又吃避孕药，血栓和心梗风险会大大增加，非常危险！")
                    .riskWarning("吸烟合并口服避孕药显著增加心血管风险，35岁以上吸烟者属禁忌！")
                    .alternatives(java.util.Arrays.asList("强烈建议戒烟", "咨询医生改用非激素避孕方法", "35岁以上吸烟者禁用此类避孕药"))
                    .build();
        }

        return null;
    }

    /**
     * 检测药品-年龄/特殊人群冲突
     */
    private DrugConflictResult checkDrugAgeConflict(String drug, Integer age) {
        if (drug == null || drug.trim().isEmpty() || age == null) {
            return null;
        }
        String drugLower = drug.toLowerCase();

        // 儿童禁用：氟喹诺酮类（影响软骨发育）
        if (age < 18 && (drugLower.contains("沙星") || drugLower.contains("诺氟沙星") || drugLower.contains("氧氟沙星")
                || drugLower.contains("环丙沙星") || drugLower.contains("左氧氟沙星"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("儿童（18岁以下）")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_AGE)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("氟喹诺酮类在动物实验中对幼年动物的承重关节软骨有毒性，18岁以下儿童使用可能影响骨骼发育。")
                    .conflictExplanation("未满18岁不能吃沙星类药物，会影响骨骼发育！")
                    .riskWarning("18岁以下儿童禁用！")
                    .alternatives(java.util.Arrays.asList("咨询医生改用儿童安全的抗生素（如阿莫西林、头孢类）"))
                    .build();
        }

        // 8岁以下儿童禁用四环素类
        if (age < 8 && (drugLower.contains("四环素") || drugLower.contains("多西环素") || drugLower.contains("米诺环素"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("8岁以下儿童")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_AGE)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("8岁以下儿童使用四环素类药物会导致牙齿永久性黄染、牙釉质发育不全，并可能影响骨骼生长。")
                    .conflictExplanation("8岁以下儿童不能吃四环素类药物，会让牙齿永久变黄！")
                    .riskWarning("8岁以下儿童禁用！")
                    .alternatives(java.util.Arrays.asList("咨询医生改用儿童安全的抗生素"))
                    .build();
        }

        // 2岁以下儿童禁用可待因/右美沙芬
        if (age < 2 && (drugLower.contains("可待因") || drugLower.contains("右美沙芬"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("2岁以下儿童")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_AGE)
                    .severity(DrugConflictResult.SeverityLevel.SEVERE)
                    .conflictMechanism("2岁以下儿童使用可待因/右美沙芬有呼吸抑制风险，甚至致死。")
                    .conflictExplanation("2岁以下儿童不能吃这类止咳药，可能抑制呼吸，非常危险！")
                    .riskWarning("2岁以下儿童禁用！")
                    .alternatives(java.util.Arrays.asList("咨询儿科医生选择儿童安全的止咳药", "注意保持室内湿度、少量多次喂水"))
                    .build();
        }

        // 老年人慎用：长半衰期苯二氮卓类
        if (age >= 65 && (drugLower.contains("地西泮") || drugLower.contains("安定") || drugLower.contains("硝西泮")
                || drugLower.contains("艾司唑仑"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("老年（65岁以上）")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_AGE)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("老年人对长半衰期苯二氮卓类药物敏感性增加、代谢减慢，容易出现过度镇静、跌倒、认知障碍等不良反应。")
                    .conflictExplanation("您年纪较大，长效安眠药会让您白天嗜睡、容易摔倒，要谨慎！")
                    .riskWarning("老年人慎用，易致跌倒和认知障碍。")
                    .alternatives(java.util.Arrays.asList("咨询医生改用短效制剂（如唑吡坦）", "从最小有效剂量开始", "注意防跌倒"))
                    .build();
        }

        // 老年人慎用：第一代抗组胺药（嗜睡/抗胆碱能）
        if (age >= 65 && (drugLower.contains("扑尔敏") || drugLower.contains("苯海拉明")
                || drugLower.contains("异丙嗪") || drugLower.contains("赛庚啶"))) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("老年（65岁以上）")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_AGE)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("第一代抗组胺药具有明显抗胆碱能作用和嗜睡作用，老年人使用增加跌倒、便秘、尿潴留、意识模糊风险。")
                    .conflictExplanation("您年纪较大，这类老式抗过敏药会让您嗜睡、便秘，还可能迷糊！")
                    .riskWarning("老年人慎用，Beers标准列入不适当用药。")
                    .alternatives(java.util.Arrays.asList("咨询医生改用第二代抗组胺药（如氯雷他定、西替利嗪）"))
                    .build();
        }

        return null;
    }

    /**
     * 检测药品-体重/剂量冲突
     */
    private DrugConflictResult checkDrugWeightConflict(String drug, java.math.BigDecimal weightKg) {
        if (drug == null || drug.trim().isEmpty() || weightKg == null) {
            return null;
        }
        // 仅作辅助提示，不阻断用药；由医生评估剂量
        // 这里只标记极端低体重（<40kg）或肥胖（BMI≥28，需配合身高，此处简化判断>90kg）
        String drugLower = drug.toLowerCase();

        // 化疗/免疫抑制剂、强效降糖药、强效降压药等需要按体重精确计量的药物
        boolean needsWeightBasedDosing = drugLower.contains("甲氨蝶呤") || drugLower.contains("环磷酰胺")
                || drugLower.contains("顺铂") || drugLower.contains("卡铂")
                || drugLower.contains("胰岛素") || drugLower.contains("肝素");

        if (!needsWeightBasedDosing) {
            return null;
        }

        if (weightKg.compareTo(java.math.BigDecimal.valueOf(40)) < 0) {
            return DrugConflictResult.builder()
                    .drugA(drug)
                    .drugB("低体重（" + weightKg + "kg）")
                    .conflictType(DrugConflictResult.ConflictType.DRUG_WEIGHT)
                    .severity(DrugConflictResult.SeverityLevel.MODERATE)
                    .conflictMechanism("该药品需根据体重计算剂量，低体重患者按常规剂量给药可能导致剂量过大。")
                    .conflictExplanation("您体重偏轻（" + weightKg + "kg），这个药通常按体重给药，请医生根据体重精确计算剂量！")
                    .riskWarning("低体重患者需严格按体重调整剂量。")
                    .alternatives(java.util.Arrays.asList("告知医生您准确的体重", "由医生精确计算剂量"))
                    .build();
        }

        return null;
    }

    /**
     * 快速冲突检测（仅使用本地规则，不调用AI）
     * 适合自动检测场景，响应速度快（毫秒级）
     *
     * @param drugNames 药品名称列表
     * @return 冲突检测报告（本地规则检测结果）
     */
    @Override
    public DrugConflictResponse quickCheckWithLocalRules(List<String> drugNames) {
        logger.info("开始快速本地冲突检测 - 药品: {}", drugNames);
        
        if (drugNames == null || drugNames.isEmpty()) {
            return createEmptyResponse(drugNames, null, null, null);
        }

        // 构建请求对象
        DrugConflictRequest request = DrugConflictRequest.builder()
                .drugNames(drugNames)
                .detailed(false)  // 不需要详细分析
                .includeAlternatives(true)
                .build();

        // 直接使用本地规则检测
        DrugConflictResponse response = analyzeWithLocalRules(request);

        // 调试日志：打印检测结果
        logger.info("快速本地冲突检测完成 - 检测到 {} 个冲突",
            response.getConflicts() != null ? response.getConflicts().size() : 0);
        if (response.getConflicts() != null && !response.getConflicts().isEmpty()) {
            for (DrugConflictResult conflict : response.getConflicts()) {
                logger.info("冲突详情: {} <-> {}, 严重程度: {}",
                    conflict.getDrugA(), conflict.getDrugB(), conflict.getSeverity());
            }
        }

        return response;
    }

    /**
     * 收集基于健康档案的 7 个新维度冲突检测结果
     * 用于在 AI 路径之后强制补充关键安全检查，避免 AI 漏检
     */
    private List<DrugConflictResult> collectProfileBasedConflicts(DrugConflictRequest request) {
        List<DrugConflictResult> profileConflicts = new ArrayList<>();
        List<String> drugNames = request.getDrugNames();
        if (drugNames == null || drugNames.isEmpty()) {
            return profileConflicts;
        }

        // 孕期冲突（is_pregnant=1 时触发）
        if (Integer.valueOf(1).equals(request.getIsPregnant())) {
            for (String drug : drugNames) {
                DrugConflictResult c = checkDrugPregnancyConflict(drug);
                if (c != null) profileConflicts.add(c);
            }
        }
        // 哺乳期冲突
        if (Integer.valueOf(1).equals(request.getIsBreastfeeding())) {
            for (String drug : drugNames) {
                DrugConflictResult c = checkDrugLactationConflict(drug);
                if (c != null) profileConflicts.add(c);
            }
        }
        // 肾功能冲突
        if (isOrganImpaired(request.getKidneyFunction())) {
            for (String drug : drugNames) {
                DrugConflictResult c = checkDrugKidneyConflict(drug, request.getKidneyFunction());
                if (c != null) profileConflicts.add(c);
            }
        }
        // 肝功能冲突
        if (isOrganImpaired(request.getLiverFunction())) {
            for (String drug : drugNames) {
                DrugConflictResult c = checkDrugLiverConflict(drug, request.getLiverFunction());
                if (c != null) profileConflicts.add(c);
            }
        }
        // 吸烟冲突
        if (Integer.valueOf(1).equals(request.getIsSmoking())) {
            for (String drug : drugNames) {
                DrugConflictResult c = checkDrugSmokingConflict(drug);
                if (c != null) profileConflicts.add(c);
            }
        }
        // 年龄/特殊人群冲突
        if (request.getAge() != null) {
            for (String drug : drugNames) {
                DrugConflictResult c = checkDrugAgeConflict(drug, request.getAge());
                if (c != null) profileConflicts.add(c);
            }
        }
        // 体重/剂量冲突
        if (request.getWeight() != null) {
            for (String drug : drugNames) {
                DrugConflictResult c = checkDrugWeightConflict(drug, request.getWeight());
                if (c != null) profileConflicts.add(c);
            }
        }

        if (!profileConflicts.isEmpty()) {
            logger.info("基于健康档案补充检测到 {} 个冲突（孕期/哺乳/肾肝/吸烟/年龄/体重）",
                profileConflicts.size());
        }
        return profileConflicts;
    }

    /**
     * 冲突结果去重（按 drugA + drugB + conflictType）
     */
    private List<DrugConflictResult> deduplicateConflicts(List<DrugConflictResult> conflicts) {
        if (conflicts == null) return new ArrayList<>();
        java.util.Map<String, DrugConflictResult> map = new java.util.LinkedHashMap<>();
        for (DrugConflictResult c : conflicts) {
            if (c == null) continue;
            String key = String.valueOf(c.getDrugA()) + "|" + String.valueOf(c.getDrugB()) + "|"
                    + String.valueOf(c.getConflictType());
            // 保留已有；如果新条目更严重（severity 数值更小=更严重）则替换
            DrugConflictResult existing = map.get(key);
            if (existing == null) {
                map.put(key, c);
            } else if (c.getSeverity() != null && existing.getSeverity() != null
                    && c.getSeverity().compareTo(existing.getSeverity()) < 0) {
                map.put(key, c);
            }
        }
        return new ArrayList<>(map.values());
    }

    @Override
    public List<DrugSearchResponse> searchMultipleDrugsWithAI(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            logger.warn("搜索关键词为空，无法调用AI搜索");
            return Collections.emptyList();
        }

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，跳过AI搜索");
            return Collections.emptyList();
        }

        try {
            logger.info("开始调用DeepSeek AI搜索多个药品 - 关键词: {}", keyword);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 2000);

            // 构建消息 - 要求AI返回多个相关药品
            String systemPrompt = "你是一个专业的药品信息查询助手。请根据用户输入的关键词，返回所有相关的药品信息。\n" +
                    "输出格式必须严格遵循以下JSON数组结构：\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"drugName\": \"药品通用名\",\n" +
                    "    \"tradeName\": \"商品名\",\n" +
                    "    \"specification\": \"规格\",\n" +
                    "    \"manufacturer\": \"生产厂家\",\n" +
                    "    \"category\": \"药品分类\"\n" +
                    "  }\n" +
                    "]\n" +
                    "重要要求：\n" +
                    "1. 返回所有包含关键词的药品，至少5-10个\n" +
                    "2. 如果某个字段不确定，请填写\"暂无详细信息\"\n" +
                    "3. 只输出JSON数组，不要有其他文字说明\n" +
                    "4. 如果关键词是单个汉字，请搜索所有包含该字的药品名称";

            String userPrompt;
            // 单字搜索时，给出更明确的提示
            if (keyword.length() == 1) {
                userPrompt = "请搜索所有药名中包含汉字'" + keyword + "'的药品。\n" +
                        "例如：如果关键词是'华'，应该返回华法林钠、华蟾素片、华佗再造丸等。\n" +
                        "请至少返回5个相关药品。";
            } else {
                userPrompt = "请搜索所有与以下关键词相关的药品：" + keyword + "\n" +
                        "例如：如果关键词是'华法'，应该返回华法林钠、华法林等相关药品。";
            }

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                List<DrugSearchResponse> results = parseMultipleDrugsResponse(responseBody);
                
                if (results != null && !results.isEmpty()) {
                    logger.info("DeepSeek AI搜索成功 - 找到 {} 个相关药品", results.size());
                    return results;
                } else {
                    logger.info("DeepSeek AI未能查询到药品信息");
                    return Collections.emptyList();
                }
            } else {
                logger.error("DeepSeek API请求失败 - 状态码: {}, 响应: {}", response.getStatusCode(), response.getBody());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            logger.error("调用DeepSeek AI搜索药品失败 - 错误: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析DeepSeek多药品搜索响应
     */
    private List<DrugSearchResponse> parseMultipleDrugsResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null) {
                        String jsonContent = content.asText().trim();
                        
                        // 尝试解析JSON数组
                        try {
                            JsonNode drugsArray = objectMapper.readTree(jsonContent);
                            
                            if (drugsArray.isArray()) {
                                List<DrugSearchResponse> results = new ArrayList<>();
                                for (JsonNode drugNode : drugsArray) {
                                    DrugSearchResponse drug = DrugSearchResponse.builder()
                                            .drugName(getJsonValue(drugNode, "drugName"))
                                            .tradeName(getJsonValue(drugNode, "tradeName"))
                                            .specification(getJsonValue(drugNode, "specification"))
                                            .manufacturer(getJsonValue(drugNode, "manufacturer"))
                                            .category(getJsonValue(drugNode, "category"))
                                            .matchScore(0.75) // AI搜索结果匹配度
                                            .matchType("ai")
                                            .build();
                                    results.add(drug);
                                }
                                return results;
                            }
                        } catch (Exception e) {
                            logger.warn("解析多药品JSON数组失败，尝试直接提取内容: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析DeepSeek多药品搜索响应失败 - 错误: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public String classifyDrugCategory(String drugName) {
        if (drugName == null || drugName.trim().isEmpty()) {
            logger.warn("药品名称为空，无法判断处方药/非处方药分类");
            return null;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，无法判断药品分类");
            return null;
        }

        try {
            logger.info("调用DeepSeek AI判断药品分类 - 药品: {}", drugName);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 10);

            String systemPrompt = "你是一个专业的药品分类助手。根据药品名称判断该药品是处方药还是非处方药。" +
                    "只需回复\"处方药\"或\"非处方药\"，不要回复任何其他内容。";

            String userPrompt = "请判断以下药品是处方药还是非处方药：\n\n" + drugName;

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String result = parseResponse(response.getBody());
                if (result != null) {
                    result = result.trim();
                    if ("处方药".equals(result) || "非处方药".equals(result)) {
                        logger.info("AI判断药品分类成功 - 药品: {}, 分类: {}", drugName, result);
                        return result;
                    }
                    // 先检查"非处方药"（更具体的关键词，包含"处方药"子串，需优先判断）
                    if (result.contains("非处方药") || result.contains("OTC")) {
                        logger.info("AI判断药品分类成功（提取） - 药品: {}, 分类: 非处方药", drugName);
                        return "非处方药";
                    }
                    if (result.contains("处方药")) {
                        logger.info("AI判断药品分类成功（提取） - 药品: {}, 分类: 处方药", drugName);
                        return "处方药";
                    }
                }
                logger.warn("AI返回的药品分类无法识别 - 药品: {}, 原始回复: {}", drugName, result);
                return null;
            } else {
                logger.error("DeepSeek API请求失败 - 状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("调用DeepSeek AI判断药品分类失败 - 药品: {}, 错误: {}", drugName, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public java.util.Map<String, String> generateDiseaseScienceLesson(String diseaseName, Integer age, String gender) {
        if (diseaseName == null || diseaseName.trim().isEmpty()) {
            logger.warn("慢病名称为空，无法生成每日科普");
            return null;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，使用本地模板生成每日科普");
            return generateLocalLesson(diseaseName, age, gender);
        }

        try {
            logger.info("开始调用DeepSeek AI生成每日慢病科普 - 疾病: {}, 年龄: {}, 性别: {}", diseaseName, age, gender);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 800);

            String systemPrompt = "你是一位专业的慢病健康科普教育师，专门为老年慢性病患者撰写通俗易懂的每日科普短文。\n\n" +
                    "请遵循以下要求：\n" +
                    "1. 语言亲切温暖，使用第二人称\"您\"，像和老人家聊天一样\n" +
                    "2. 避免专业医学术语，用通俗比喻解释\n" +
                    "3. 科普正文结构：\n" +
                    "   - 【小知识】用1-2句话介绍这个慢病的一个关键知识点\n" +
                    "   - 【生活小贴士】给出1-2条实用的日常生活建议（饮食、运动、作息等）\n" +
                    "   - 【温馨提醒】1条简短提醒\n" +
                    "4. 总字数控制在200-350字之间，不要太长\n" +
                    "5. 输出严格的JSON格式：{\"title\": \"科普标题\", \"content\": \"科普正文\"}\n" +
                    "6. 标题要有吸引力，用1-2个emoji点缀（如💊🥗🏃‍♂️⚠️等）\n" +
                    "7. 每天都换一个不同的角度切入，不要重复\n" +
                    "8. 正文中不要使用emoji，保持文字干净";

            String genderText = "male".equalsIgnoreCase(gender) ? "男性" :
                    "female".equalsIgnoreCase(gender) ? "女性" : "长者";
            String ageText = age != null ? age + "岁" : "长者";

            String userPrompt = "请为一位" + ageText + "的" + genderText + "慢性病患者撰写今日科普，其慢性病为：" + diseaseName + "。\n\n" +
                    "今天的科普内容要求：围绕\"" + diseaseName + "\"的一个日常管理要点展开，给出实用建议。";

            // RAG 检索增强：注入该慢病的知识库资料，科普内容基于资料而非纯模型记忆
            String context = ragSearchService.formatContext(diseaseName, 3);
            if (!context.isEmpty()) {
                userPrompt = context + "\n" + userPrompt
                        + "\n\n请优先参考上述参考资料中的管理要点，用你自己的话通俗表达。";
            }

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                String content = parseResponse(responseBody);

                if (content != null && !content.isEmpty()) {
                    Map<String, String> result = parseLessonJson(content);
                    if (result != null) {
                        logger.info("DeepSeek AI生成每日科普成功 - 标题: {}", result.get("title"));
                        return result;
                    }
                    // JSON解析失败，尝试从纯文本提取
                    Map<String, String> fallback = new HashMap<>();
                    fallback.put("title", diseaseName + "日常管理小知识");
                    // 如果content是JSON字符串，尝试提取content字段
                    String displayContent = content;
                    try {
                        JsonNode inner = objectMapper.readTree(content);
                        if (inner.has("content")) {
                            displayContent = inner.get("content").asText();
                        }
                        if (inner.has("title")) {
                            fallback.put("title", inner.get("title").asText());
                        }
                    } catch (Exception ignored) {}
                    fallback.put("content", displayContent.length() > 800 ? displayContent.substring(0, 800) : displayContent);
                    return fallback;
                }
            }
            logger.error("DeepSeek API每日科普请求失败 - 状态码: {}", response.getStatusCode());
            return generateLocalLesson(diseaseName, age, gender);

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("调用DeepSeek AI生成每日科普失败 - HTTP状态码: {}, 响应体: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return generateLocalLesson(diseaseName, age, gender);
        } catch (Exception e) {
            logger.error("调用DeepSeek AI生成每日科普失败 - 错误: {}", e.getMessage(), e);
            return generateLocalLesson(diseaseName, age, gender);
        }
    }

    /**
     * 解析每日科普的JSON响应
     */
    private java.util.Map<String, String> parseLessonJson(String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            return null;
        }

        // 清理markdown代码块包裹
        String cleaned = jsonContent.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        // 方法1：尝试标准JSON解析
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            String title = getJsonString(root, "title");
            String content = getJsonString(root, "content");
            if (title != null && content != null && title.length() >= 2 && content.length() >= 20) {
                Map<String, String> result = new HashMap<>();
                result.put("title", title.trim());
                result.put("content", content.trim());
                return result;
            }
        } catch (Exception e) {
            logger.warn("标准JSON解析失败，使用正则兜底: {}", e.getMessage());
        }

        // 方法2：正则兜底（应对AI返回的JSON中含有未转义引号的情况）
        try {
            String title = extractJsonField(cleaned, "title");
            String content = extractJsonField(cleaned, "content");
            if (title != null && content != null && title.length() >= 2 && content.length() >= 20) {
                logger.info("使用正则方式成功解析每日科普");
                Map<String, String> result = new HashMap<>();
                result.put("title", title.trim());
                result.put("content", content.trim());
                return result;
            }
        } catch (Exception e) {
            logger.warn("正则解析每日科普JSON失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 使用正则提取JSON字段值（兜底方案）
     */
    private String extractJsonField(String json, String fieldName) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"" + java.util.regex.Pattern.quote(fieldName) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
                java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String value = matcher.group(1);
            return value.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
        }
        return null;
    }

    /**
     * 本地生成每日科普（当AI不可用时使用）
     */
    private java.util.Map<String, String> generateLocalLesson(String diseaseName, Integer age, String gender) {
        Map<String, String> result = new HashMap<>();

        // 常见慢病的本地科普模板
        String key = diseaseName.trim();
        String title;
        String content;

        if (key.contains("高血压") || key.contains("血压")) {
            title = "💓 控制血压，从每天的小习惯开始";
            content = "【小知识】高血压被称为「沉默的杀手」，因为它在早期往往没有明显症状，但长期高血压会悄悄损伤心脏、大脑和肾脏。\n\n" +
                    "【生活小贴士】每天食盐摄入量不要超过一啤酒瓶盖（约5克），做菜时可以用醋、葱姜蒜来调味，减少酱油和味精的使用。每天快走30分钟，微微出汗就好，对降压特别有帮助。\n\n" +
                    "【温馨提醒】请您每天固定时间测量血压并记录下来，如果连续几天血压偏高，记得及时联系医生。";
        } else if (key.contains("糖尿病") || key.contains("血糖")) {
            title = "🍚 糖尿病不可怕，管住嘴迈开腿";
            content = "【小知识】糖尿病的管理核心是控制血糖波动，血糖大起大落比单纯血糖高更伤身体。\n\n" +
                    "【生活小贴士】吃饭顺序很重要：先喝汤，再吃蔬菜，然后吃蛋白质（鱼、肉、蛋），最后吃主食。这个顺序可以延缓血糖上升。水果选择苹果、柚子、草莓等低糖水果，放在两餐之间吃。\n\n" +
                    "【温馨提醒】请您定期检查双脚，看有没有伤口或水泡，糖尿病人的足部护理非常重要。";
        } else if (key.contains("冠心病") || key.contains("心脏")) {
            title = "❤️ 护心养心，从日常生活做起";
            content = "【小知识】冠心病是给心脏供血的血管变窄了，就像水管里长了水垢。情绪激动、劳累、寒冷都会让血管突然变窄，引发心绞痛。\n\n" +
                    "【生活小贴士】起床时不要猛地坐起来，先在床上躺一分钟，坐一分钟，再把脚放下来坐一分钟，这叫做「三个半分钟」，可以预防起床时的心脑血管意外。保持大便通畅也很重要，用力排便会加重心脏负担。\n\n" +
                    "【温馨提醒】请您随身带好急救药（如速效救心丸），出门时告诉家人您的去向。";
        } else if (key.contains("高血脂") || key.contains("血脂")) {
            title = "🥑 降血脂不只是少吃油";
            content = "【小知识】血脂高不全是吃油多造成的，身体自己合成的胆固醇占了大头。有些人即使吃得很清淡，血脂还是高，这和遗传、代谢都有关系。\n\n" +
                    "【生活小贴士】多吃富含膳食纤维的食物：燕麦、红薯、豆类、海带，它们像「小刷子」一样帮您把多余的胆固醇带出体外。每周吃两次鱼，鱼油中的Omega-3对心血管有益。\n\n" +
                    "【温馨提醒】降脂药需要长期坚持服用才能看到效果，不要因为感觉没事就自己停药。";
        } else if (key.contains("脑梗死") || key.contains("脑梗") || key.contains("中风")) {
            title = "🧠 预防脑梗，警惕这些信号";
            content = "【小知识】脑梗死是因为脑部血管被堵住了，脑细胞缺血会很快死亡。记住「FAST」口诀：脸歪、手无力、说话不清，出现任何一个都要立刻打120。\n\n" +
                    "【生活小贴士】控制好血压是预防脑梗最重要的事。每天喝够水，避免血液黏稠。冬天出门注意保暖，寒冷会让血管收缩。保持情绪平稳，大喜大悲都是诱因。\n\n" +
                    "【温馨提醒】如果您有房颤，一定要按医嘱服用抗凝药，房颤是脑梗的重要诱因。";
        } else if (key.contains("肾病") || key.contains("肾功")) {
            title = "🫘 保护肾脏，从日常细节做起";
            content = "【小知识】慢性肾病早期往往没有明显症状，很多人发现时已经到了中晚期。肾脏就像身体的「净水器」，一旦损坏很难恢复。\n\n" +
                    "【生活小贴士】不要乱吃止痛药，布洛芬等非甾体消炎药长期服用会伤肾。每天喝水1500-2000毫升，但不要一次猛喝。少吃高盐高蛋白食物，减轻肾脏过滤负担。\n\n" +
                    "【温馨提醒】请您定期检查尿常规和肾功能指标，早发现早干预是关键。";
        } else if (key.contains("肝病") || key.contains("肝功") || key.contains("肝炎")) {
            title = "🫁 养肝护肝，这些习惯很重要";
            content = "【小知识】肝脏是身体的「化工厂」，负责解毒、代谢和储存营养。肝脏没有痛觉神经，等到不舒服时往往已经损伤较重了。\n\n" +
                    "【生活小贴士】绝对戒酒是护肝第一要务。不要熬夜，晚上11点前入睡让肝脏充分休息。少吃发霉食物，黄曲霉素是伤肝的隐形杀手。谨慎使用保健品，有些反而加重肝脏负担。\n\n" +
                    "【温馨提醒】请您每半年做一次肝功能和B超检查，有乙肝病史的更要注意。";
        } else if (key.contains("哮喘")) {
            title = "🌬️ 管控哮喘，呼吸更自由";
            content = "【小知识】哮喘是气道慢性炎症，遇到刺激就会痉挛变窄，导致呼吸困难。它不能根治，但规范治疗可以完全控制。\n\n" +
                    "【生活小贴士】找出并远离您的诱发因素：尘螨、花粉、冷空气、油烟等。随身携带缓解药物（如沙丁胺醇吸入剂），发作时能及时自救。坚持使用控制性药物，不要因为不喘了就停药。\n\n" +
                    "【温馨提醒】请您学会使用峰流速仪自我监测，数值下降时提前加药可以预防发作。";
        } else if (key.contains("慢阻肺") || key.contains("COPD")) {
            title = "🫁 慢阻肺患者，呼吸训练很重要";
            content = "【小知识】慢阻肺是长期吸烟或接触有害气体导致的肺部不可逆损伤，表现为持续咳嗽、咳痰和活动后气喘。\n\n" +
                    "【生活小贴士】练习缩唇呼吸：用鼻子深吸气2秒，嘴唇缩成吹口哨状慢慢呼气4-6秒，每天练习3次，每次10分钟。坚决戒烟，远离二手烟和厨房油烟。秋冬季节注意保暖防感冒，感冒是急性加重的常见诱因。\n\n" +
                    "【温馨提醒】请您按时吸入维持药物，每年接种流感疫苗和肺炎疫苗。";
        } else if (key.contains("痛风") || key.contains("尿酸")) {
            title = "🦶 痛风发作太痛苦，饮食管理是关键";
            content = "【小知识】痛风是尿酸结晶沉积在关节引起的剧烈疼痛，大脚趾是最常见的发作部位。尿酸高不一定马上发作，但长期不控制会损伤肾脏。\n\n" +
                    "【生活小贴士】少喝肉汤和海鲜，它们嘌呤含量高。多喝水，每天2000毫升以上帮助排尿酸。绝对不喝啤酒，啤酒是痛风的「催化剂」。樱桃有助降尿酸，可以适量吃。\n\n" +
                    "【温馨提醒】急性发作时不要自行停用降尿酸药，请遵医嘱调整用药。";
        } else if (key.contains("骨质疏松")) {
            title = "🦴 预防骨质疏松，从补钙开始";
            content = "【小知识】骨质疏松让骨头变得像「蜂窝煤」一样脆弱，轻微碰撞甚至咳嗽都可能导致骨折。绝经后的女性风险更高。\n\n" +
                    "【生活小贴士】每天晒太阳15-20分钟，帮助身体合成维生素D促进钙吸收。牛奶、豆腐、绿叶菜是最好的钙来源。适当做负重运动如散步、太极，刺激骨骼保持强度。防跌倒很重要，家里保持地面干燥、光线充足。\n\n" +
                    "【温馨提醒】补钙不是越多越好，请在医生指导下合理补充钙剂和维生素D。";
        } else if (key.contains("心律失常") || key.contains("心律")) {
            title = "💓 心跳不规律，这些事要注意";
            content = "【小知识】心律失常就是心跳的节奏出了问题，可能太快、太慢或不规则。偶尔的心慌不用紧张，但频繁发作需要重视。\n\n" +
                    "【生活小贴士】减少咖啡和浓茶的摄入，它们可能诱发心律失常。保持规律作息，熬夜和过度疲劳是常见诱因。学会自测脉搏：安静状态下每分钟60-100次是正常的。\n\n" +
                    "【温馨提醒】如果您感觉心跳突然变得又快又乱，伴有头晕、胸闷，请立即就医。";
        } else if (key.contains("心力衰竭") || key.contains("心衰")) {
            title = "❤️ 心衰患者，体重管理很重要";
            content = "【小知识】心力衰竭不是心脏停跳了，而是心脏泵血能力下降，不能满足身体需要。常见表现是活动后气喘、下肢浮肿和疲劳。\n\n" +
                    "【生活小贴士】每天早晨排尿后称体重，如果3天内体重增加超过2公斤，可能体内积液增多了，要及时联系医生。控制饮水量，每天不超过1500毫升。少吃盐，避免体内水分滞留。\n\n" +
                    "【温馨提醒】心衰药物需要长期坚持服用，不要因为症状好转就自行减量。";
        } else if (key.contains("帕金森")) {
            title = "🧠 帕金森病，坚持锻炼很重要";
            content = "【小知识】帕金森病是大脑中多巴胺分泌减少导致的运动障碍，主要表现为手抖、动作变慢和身体僵硬。它虽然无法治愈，但药物和锻炼可以显著改善生活质量。\n\n" +
                    "【生活小贴士】坚持每天走路和做拉伸运动，有助于维持关节灵活性和平衡能力。大步走、摆臂练习对改善步态特别有效。饮食上多吃富含纤维的食物，便秘是帕金森常见的困扰。\n\n" +
                    "【温馨提醒】请严格按时服药，漏服或自行调整时间可能导致症状波动。";
        } else if (key.contains("类风湿")) {
            title = "🤲 类风湿关节炎，早治是关键";
            content = "【小知识】类风湿关节炎是免疫系统错误攻击自身关节导致的慢性炎症，常见于手指、手腕等小关节，早晨起来关节僵硬是典型表现。\n\n" +
                    "【生活小贴士】急性期要休息，缓解期要适当活动关节，完全不动反而会加重僵硬。温水泡手可以缓解晨僵。保护小关节：用掌心拧瓶盖而不是用手指，用粗柄的餐具更省力。\n\n" +
                    "【温馨提醒】类风湿需要长期规范治疗，越早治疗关节损伤越小，不要等到变形了才就医。";
        } else {
            title = "📚 " + diseaseName + " —— 日常管理小知识";
            content = "【小知识】" + diseaseName + "是一种常见的慢性疾病，需要长期坚持管理。良好的生活习惯和规律的治疗同样重要。\n\n" +
                    "【生活小贴士】保持规律作息，每天适量运动，饮食均衡清淡，多与家人朋友交流，保持心情愉快。按时服药，不要自行停药或调整剂量。\n\n" +
                    "【温馨提醒】请您遵医嘱定期复查，如有不适及时就诊，不要拖延。";
        }

        result.put("title", title);
        result.put("content", content);
        return result;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，RAG生成跳过");
            return null;
        }
        if (systemPrompt == null || userPrompt == null || userPrompt.trim().isEmpty()) {
            logger.warn("chat 入参为空，跳过调用");
            return null;
        }
        // 纵深防御：prompt 注入 / 危险请求不进入 LLM（上层入口已拦截，此处兜底）
        if (!SafetyGuard.isSafe(userPrompt)) {
            logger.warn("SafetyGuard 拦截注入请求，跳过 LLM 调用");
            return null;
        }
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 1500);

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);

            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseResponse(response.getBody());
            }
            logger.error("DeepSeek chat 请求失败 - 状态码: {}, 响应: {}", response.getStatusCode(), response.getBody());
            return null;
        } catch (Exception e) {
            logger.error("调用 DeepSeek chat 失败 - 错误: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void chatStream(String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("DeepSeek API Key未配置，流式生成跳过");
            return;
        }
        if (systemPrompt == null || userPrompt == null || userPrompt.trim().isEmpty()) {
            return;
        }
        // 纵深防御：prompt 注入 / 危险请求不进入 LLM
        if (!SafetyGuard.isSafe(userPrompt)) {
            logger.warn("SafetyGuard 拦截注入请求，跳过流式调用");
            return;
        }
        HttpURLConnection conn = null;
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 1500);
            requestBody.put("stream", true);

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            byte[] body = objectMapper.writeValueAsBytes(requestBody);
            URL url = new URL(DEEPSEEK_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(120000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            conn.setDoOutput(true);
            conn.getOutputStream().write(body);

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }
                // 逐块提取增量内容：data: {"choices":[{"delta":{"content":"..."}}]}
                JsonNode node = objectMapper.readTree(data);
                JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                if (!delta.isMissingNode() && !delta.isNull()) {
                    String text = delta.asText();
                    if (!text.isEmpty()) {
                        onDelta.accept(text);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("DeepSeek chat 流式调用失败 - 错误: {}", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
