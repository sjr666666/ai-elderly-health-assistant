package com.example.backend.service.impl;

import com.example.backend.config.BaiduTtsConfig;
import com.example.backend.service.BaiduTtsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 百度语音合成服务实现类
 */
@Service
public class BaiduTtsServiceImpl implements BaiduTtsService {

    private static final Logger logger = LoggerFactory.getLogger(BaiduTtsServiceImpl.class);

    private static final String ACCESS_TOKEN_CACHE_KEY = "baidu:tts:access_token";
    private static final long TOKEN_EXPIRE_SECONDS = 2592000; // 30天

    @Autowired
    private BaiduTtsConfig ttsConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private String accessToken;

    @PostConstruct
    public void init() {
        logger.info("百度TTS配置 - appId: {}, apiKey: {}, secretKey: {}",
                ttsConfig.getAppId(),
                ttsConfig.getApiKey(),
                ttsConfig.getSecretKey() != null ? "*****" : "null");

        // 尝试从Redis获取access token
        if (redisTemplate != null) {
            try {
                accessToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
                if (accessToken != null && !accessToken.isEmpty()) {
                    logger.info("从Redis缓存获取百度TTS access token成功");
                    return;
                }
            } catch (Exception e) {
                logger.warn("从Redis获取access token失败，使用临时token: {}", e.getMessage());
            }
        }

        // 获取新的access token
        accessToken = getAccessToken();
    }

    /**
     * 获取百度Access Token
     */
    private String getAccessToken() {
        try {
            // 直接从配置获取，不依赖@PostConstruct
            String apiKey = ttsConfig.getApiKey();
            String secretKey = ttsConfig.getSecretKey();

            logger.info("开始获取百度Access Token - apiKey: {}, secretKey: {}",
                    apiKey != null ? "*****" : "null",
                    secretKey != null ? "*****" : "null");

            if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
                logger.error("百度API配置为空 - apiKey: {}, secretKey: {}", apiKey, secretKey);
                return null;
            }

            String url = ttsConfig.getAccessTokenUrl() +
                    "?grant_type=client_credentials" +
                    "&client_id=" + apiKey +
                    "&client_secret=" + secretKey;

            logger.info("正在请求百度Access Token...");

            String response = restTemplate.getForObject(url, String.class);
            logger.info("百度Token响应: {}", response);

            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("access_token")) {
                String token = jsonNode.get("access_token").asText();
                logger.info("成功获取百度Access Token");

                // 缓存到Redis
                if (redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, token, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
                        logger.info("Access token已缓存到Redis");
                    } catch (Exception e) {
                        logger.warn("缓存access token到Redis失败: {}", e.getMessage());
                    }
                }

                return token;
            } else {
                logger.error("获取百度Access Token失败: {}", response);
                return null;
            }
        } catch (Exception e) {
            logger.error("获取Access Token异常: ", e);
            return null;
        }
    }

    @Override
    public String textToSpeech(String text, int speechRate) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("文本为空，无法转换");
            return null;
        }

        // 确保有access token
        if (accessToken == null || accessToken.isEmpty()) {
            accessToken = getAccessToken();
            if (accessToken == null) {
                logger.error("无法获取Access Token");
                return null;
            }
        }

        try {
            // 构建请求参数
            StringBuilder params = new StringBuilder();
            params.append("tok=").append(accessToken);
            params.append("&tex=").append(java.net.URLEncoder.encode(text, "UTF-8"));
            params.append("&per=").append(ttsConfig.getPer()); // 0为女声
            params.append("&spd=").append(speechRate); // 语速
            params.append("&pit=").append(ttsConfig.getPitch()); // 音调
            params.append("&vol=").append(ttsConfig.getVolume()); // 音量
            params.append("&aue=").append(ttsConfig.getAue()); // 音频格式，6为mp3
            params.append("&cuid=").append(ttsConfig.getAppId());
            params.append("&lan=zh");
            params.append("&ctp=1");

            // 发送请求
            String url = ttsConfig.getTtsUrl() + "?" + params.toString();
            byte[] audioData = restTemplate.getForObject(url, byte[].class);

            if (audioData != null && audioData.length > 0) {
                // 检查是否是错误响应（百度返回错误时是JSON格式）
                String responseStr = new String(audioData);
                if (responseStr.startsWith("{")) {
                    JsonNode errorNode = objectMapper.readTree(responseStr);
                    if (errorNode.has("err_msg")) {
                        logger.error("百度TTS API错误: {}", errorNode.get("err_msg").asText());
                        return null;
                    }
                }

                // 将音频数据转换为Base64
                String base64Audio = Base64.getEncoder().encodeToString(audioData);
                logger.info("文本转语音成功，文本长度: {}, 音频大小: {} bytes", text.length(), audioData.length);

                return "data:audio/mp3;base64," + base64Audio;
            } else {
                logger.error("百度TTS返回空数据");
                return null;
            }

        } catch (Exception e) {
            logger.error("文本转语音异常: ", e);

            // 如果是token过期，尝试重新获取
            if (e.getMessage() != null && e.getMessage().contains("access_token")) {
                accessToken = getAccessToken();
            }

            return null;
        }
    }

    @Override
    public MultipartFile textToSpeechFile(String text, int speechRate) {
        String base64Audio = textToSpeech(text, speechRate);

        if (base64Audio == null) {
            return null;
        }

        try {
            // 移除前缀
            String base64Data = base64Audio.replace("data:audio/mp3;base64,", "");
            byte[] audioBytes = Base64.getDecoder().decode(base64Data);

            // 创建临时文件
            Path tempFile = Files.createTempFile("tts_", ".mp3");
            Files.write(tempFile, audioBytes);

            return new TempMultipartFile(tempFile.toFile(), "audio/mp3", "speech.mp3");

        } catch (Exception e) {
            logger.error("创建音频文件失败: ", e);
            return null;
        }
    }

    /**
     * 临时文件MultipartFile实现类
     */
    private static class TempMultipartFile implements org.springframework.web.multipart.MultipartFile {
        private final File file;
        private final String contentType;
        private final String originalFilename;

        public TempMultipartFile(File file, String contentType, String originalFilename) {
            this.file = file;
            this.contentType = contentType;
            this.originalFilename = originalFilename;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(file.toPath());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.copy(file.toPath(), dest.toPath());
        }
    }
}
