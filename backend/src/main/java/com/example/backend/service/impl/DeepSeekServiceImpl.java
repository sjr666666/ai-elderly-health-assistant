package com.example.backend.service.impl;

import com.example.backend.model.dto.DrugConflictRequest;
import com.example.backend.model.dto.DrugConflictResponse;
import com.example.backend.model.dto.DrugConflictResult;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.service.DeepSeekService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public DeepSeekServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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

        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek API Key未配置，跳过AI查询");
            return null;
        }

        try {
            logger.info("开始调用DeepSeek AI查询药品信息 - 药品名称: {}", drugName);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 1000);

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
                    "如果某个字段不确定，请填写\"暂无详细信息\"。";

            String userPrompt = "请查询以下药品的详细信息：" + drugName;

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
                        
                        // 尝试解析JSON
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
                            logger.warn("解析药品信息JSON失败，尝试直接提取内容: {}", e.getMessage());
                            // 如果不是JSON格式，直接返回解析后的文本
                            return DrugDetailResponse.builder()
                                    .genericName(jsonContent)
                                    .ingredient("暂无详细信息")
                                    .indications("暂无详细信息")
                                    .usage("暂无详细信息")
                                    .precautions("暂无详细信息")
                                    .adverseReactions("暂无详细信息")
                                    .description(jsonContent)
                                    .build();
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
     * 获取JSON节点值，不存在则返回默认值
     */
    private String getJsonValue(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName)) {
            String value = node.get(fieldName).asText();
            return value != null && !value.isEmpty() && !"null".equalsIgnoreCase(value) ? value : "暂无详细信息";
        }
        return "暂无详细信息";
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
            requestBody.put("max_tokens", 2000);

            // 构建系统提示词
            String systemPrompt = buildConflictSystemPrompt(request.isDetailed(), request.isIncludeAlternatives());

            // 构建用户提示词
            String userPrompt = buildConflictUserPrompt(request);

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

            // 发送请求
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
                            result.setConflicts(new java.util.ArrayList<>());
                        }
                        for (DrugConflictResult alcoholConflict : alcoholConflicts) {
                            boolean exists = result.getConflicts().stream()
                                .anyMatch(c -> c.getDrugA().equals(alcoholConflict.getDrugA()) 
                                    && c.getDrugB().equals(alcoholConflict.getDrugB()));
                            if (!exists) {
                                result.getConflicts().add(alcoholConflict);
                            }
                        }
                        // 更新统计信息
                        result.setHasSevereConflict(result.getConflicts().stream()
                            .anyMatch(c -> c.getSeverity() == DrugConflictResult.SeverityLevel.SEVERE));
                        result.setStatistics(buildStatistics(result.getConflicts()));
                    }
                    
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
            logger.error("调用DeepSeek AI进行冲突检测失败 - 错误: {}", e.getMessage(), e);
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
        prompt.append("      \"conflictType\": \"DRUG_DRUG|DRUG_FOOD|DRUG_BEVERAGE|DRUG_SUPPLEMENT\",\n");
        prompt.append("      \"severity\": \"SEVERE|MODERATE|MILD\",\n");
        prompt.append("      \"conflictMechanism\": \"专业的冲突原理描述\",\n");
        prompt.append("      \"conflictExplanation\": \"用通俗易懂的语言解释冲突\",\n");
        prompt.append("      \"riskWarning\": \"风险提示\",\n");
        prompt.append("      \"alternatives\": [\"替代方案1\", \"替代方案2\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"generalAdvice\": \"总体用药建议\"\n");
        prompt.append("}\n\n");
        prompt.append("冲突严重程度说明：\n");
        prompt.append("- SEVERE（重度）：禁止同时使用，可能导致严重不良反应或危及生命\n");
        prompt.append("- MODERATE（中度）：谨慎使用，可能加重副作用或降低药效\n");
        prompt.append("- MILD（轻度）：可以使用，但需要注意观察身体反应\n\n");
        prompt.append("冲突类型说明：\n");
        prompt.append("- DRUG_DRUG：药品与药品之间的冲突\n");
        prompt.append("- DRUG_FOOD：药品与食物之间的冲突\n");
        prompt.append("- DRUG_BEVERAGE：药品与饮料（如酒精、咖啡、茶等）之间的冲突\n");
        prompt.append("- DRUG_SUPPLEMENT：药品与保健品之间的冲突\n\n");
        
        if (detailed) {
            prompt.append("请提供详细的冲突原理和解释，包括药理机制。\n");
        }
        
        if (includeAlternatives) {
            prompt.append("请为存在冲突的组合提供合理的替代方案建议。\n");
        }
        
        prompt.append("如果没有检测到冲突，conflicts数组应为空数组[]。\n");
        prompt.append("输出必须是严格的JSON格式，不能包含任何其他文本。");
        
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
        
        prompt.append("\n请检测所有可能的组合，包括：\n");
        prompt.append("1. 药品与药品之间的相互作用\n");
        prompt.append("2. 药品与保健品之间的相互作用\n");
        prompt.append("3. 药品与饮料之间的相互作用\n");
        prompt.append("4. 药品与食物之间的相互作用\n");
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
                            JsonNode resultJson = objectMapper.readTree(jsonContent);
                            
                            List<DrugConflictResult> conflicts = new java.util.ArrayList<>();
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
                            return null;
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
            
            List<String> alternatives = new java.util.ArrayList<>();
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
                .conflicts(conflicts)
                .hasSevereConflict(severeCount > 0)
                .statistics(statistics)
                .generalAdvice(generalAdvice)
                .complete(true)
                .build();
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
                .drugsChecked(drugs != null ? drugs : new java.util.ArrayList<>())
                .supplementsChecked(supplements != null ? supplements : new java.util.ArrayList<>())
                .beveragesChecked(beverages != null ? beverages : new java.util.ArrayList<>())
                .foodsChecked(foods != null ? foods : new java.util.ArrayList<>())
                .conflicts(new java.util.ArrayList<>())
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
        
        List<DrugConflictResult> conflicts = new java.util.ArrayList<>();
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
        
        String generalAdvice = conflicts.isEmpty() 
            ? "未检测到明显的药品冲突，但仍建议在医生或药师指导下使用。"
            : "检测到药品冲突，请务必咨询医生或药师后再使用。";
        
        return buildResponse(request, conflicts, generalAdvice);
    }

    /**
     * 检测药品-药品冲突（本地规则）
     */
    private DrugConflictResult checkDrugDrugConflict(String drugA, String drugB) {
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
             drugBLower.contains("双氯芬酸") || drugBLower.contains("消炎痛"))) {
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
     * 检测药品-酒精冲突（本地规则）
     */
    private DrugConflictResult checkAlcoholConflict(String drug) {
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
     * 检测药品-保健品冲突（本地规则）
     */
    private DrugConflictResult checkDrugSupplementConflict(String drug, String supplement) {
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
     * 检测药品-食物冲突（本地规则）
     */
    private DrugConflictResult checkDrugFoodConflict(String drug, String food) {
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
        List<DrugConflictResult> conflicts = new java.util.ArrayList<>();
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
}
