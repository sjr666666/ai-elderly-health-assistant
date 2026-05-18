package com.example.backend.service.impl;

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
}