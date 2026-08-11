package com.example.backend.service.impl;

import com.example.backend.config.BaiduAsrConfig;
import com.example.backend.service.BaiduAsrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 百度语音识别（ASR）服务实现
 * <p>
 * 链路：access_token（Redis 缓存）→ 音频 base64 提交短语音识别 → 返回文字。
 * 未配 Key 时 isConfigured()=false，前端自动降级浏览器 Web Speech API，功能不中断。
 */
@Service
public class BaiduAsrServiceImpl implements BaiduAsrService {

    private static final Logger logger = LoggerFactory.getLogger(BaiduAsrServiceImpl.class);
    private static final String ACCESS_TOKEN_CACHE_KEY = "baidu:asr:access_token";
    private static final long TOKEN_EXPIRE_SECONDS = 2592000; // 百度 token 有效期 30 天

    @Autowired private BaiduAsrConfig asrConfig;
    @Autowired @Qualifier("aiRestTemplate") private RestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired(required = false) private StringRedisTemplate redisTemplate;
    private volatile String accessToken;

    @Override
    public boolean isConfigured() {
        return hasText(asrConfig.getApiKey()) && hasText(asrConfig.getSecretKey());
    }

    @Override
    public String recognize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            logger.warn("ASR 音频为空");
            return null;
        }
        if (!isConfigured()) {
            logger.warn("百度 ASR 未配置（缺 api-key / secret-key），无法识别");
            return null;
        }
        try {
            byte[] audio = file.getBytes();
            if (audio.length > 4 * 1024 * 1024) {
                logger.warn("ASR 音频超过 4MB 限制，长度: {}", audio.length);
                return null;
            }
            if (!hasText(accessToken)) {
                accessToken = fetchAccessToken();
                if (!hasText(accessToken)) {
                    return null;
                }
            }

            String format = asrConfig.getFormat();
            String name = file.getOriginalFilename();
            if (name != null && name.toLowerCase().endsWith(".pcm")) {
                format = "pcm";
            }

            Map<String, Object> body = new HashMap<>();
            body.put("format", format);
            body.put("rate", asrConfig.getRate());
            body.put("channel", 1);
            body.put("cuid", "elderly-health-assistant");
            body.put("len", audio.length);
            body.put("speech", Base64.getEncoder().encodeToString(audio));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = asrConfig.getAsrUrl() + "?access_token=" + accessToken;

            logger.info("调用百度短语音识别 - 格式: {}, 采样率: {}, 音频长度: {} 字节", format, asrConfig.getRate(), audio.length);
            String response = restTemplate.postForObject(url, request, String.class);
            if (response == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response);
            if (root.path("err_no").asInt(0) != 0) {
                logger.error("百度 ASR 识别失败 - err_no: {}, err_msg: {}",
                        root.path("err_no").asText(), root.path("err_msg").asText());
                return null;
            }
            JsonNode result = root.path("result");
            if (result.isArray() && result.size() > 0) {
                String text = result.get(0).asText().trim();
                logger.info("百度 ASR 识别成功 - 文字: {}", text);
                return text;
            }
            return null;
        } catch (Exception e) {
            logger.error("百度 ASR 调用异常: {}", e.getMessage(), e);
            return null;
        }
    }

    private String fetchAccessToken() {
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
                if (hasText(cached)) {
                    logger.info("从 Redis 缓存获取百度 ASR access token");
                    return cached;
                }
            } catch (Exception e) {
                logger.warn("从 Redis 获取 ASR token 失败: {}", e.getClass().getSimpleName());
            }
        }
        try {
            String url = asrConfig.getAccessTokenUrl() + "?grant_type=client_credentials"
                    + "&client_id=" + asrConfig.getApiKey()
                    + "&client_secret=" + asrConfig.getSecretKey();
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            if (jsonNode.has("access_token")) {
                String token = jsonNode.get("access_token").asText();
                if (redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, token,
                                TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.warn("缓存 ASR token 失败: {}", e.getClass().getSimpleName());
                    }
                }
                return token;
            }
            logger.error("获取百度 ASR access token 失败: {}", response);
            return null;
        } catch (Exception e) {
            logger.error("获取百度 ASR access token 异常: {}", e.getMessage());
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
