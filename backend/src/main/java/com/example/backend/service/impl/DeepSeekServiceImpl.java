package com.example.backend.service.impl;

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
}