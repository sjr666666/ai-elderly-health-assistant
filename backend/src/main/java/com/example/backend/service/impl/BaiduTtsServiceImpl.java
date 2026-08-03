package com.example.backend.service.impl;

import com.example.backend.config.BaiduTtsConfig;
import com.example.backend.service.BaiduTtsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

/** 百度语音合成服务实现类 */
@Service
public class BaiduTtsServiceImpl implements BaiduTtsService {

    private static final Logger logger = LoggerFactory.getLogger(BaiduTtsServiceImpl.class);
    private static final String ACCESS_TOKEN_CACHE_KEY = "baidu:tts:access_token";
    private static final long TOKEN_EXPIRE_SECONDS = 2592000;

    @Autowired private BaiduTtsConfig ttsConfig;
    @Autowired @Qualifier("ttsRestTemplate") private RestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired(required = false) private StringRedisTemplate redisTemplate;
    private String accessToken;

    @PostConstruct
    public void init() {
        logger.info("百度TTS配置已加载 - appIdPresent: {}, apiKeyPresent: {}, secretKeyPresent: {}",
                hasText(ttsConfig.getAppId()), hasText(ttsConfig.getApiKey()), hasText(ttsConfig.getSecretKey()));
        if (redisTemplate != null) {
            try {
                accessToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_CACHE_KEY);
                if (hasText(accessToken)) {
                    logger.info("从Redis缓存获取百度TTS access token成功");
                    return;
                }
            } catch (Exception e) {
                logger.warn("从Redis获取access token失败，将尝试重新获取: {}", e.getClass().getSimpleName());
            }
        }
        accessToken = getAccessToken();
    }

    private String getAccessToken() {
        try {
            String apiKey = ttsConfig.getApiKey();
            String secretKey = ttsConfig.getSecretKey();
            if (!hasText(apiKey) || !hasText(secretKey)) {
                logger.error("百度API配置不完整 - apiKeyPresent: {}, secretKeyPresent: {}",
                        hasText(apiKey), hasText(secretKey));
                return null;
            }
            String url = ttsConfig.getAccessTokenUrl() + "?grant_type=client_credentials"
                    + "&client_id=" + apiKey + "&client_secret=" + secretKey;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            if (jsonNode.has("access_token")) {
                String token = jsonNode.get("access_token").asText();
                if (redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set(ACCESS_TOKEN_CACHE_KEY, token,
                                TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.warn("缓存access token失败: {}", e.getClass().getSimpleName());
                    }
                }
                return token;
            }
            logger.error("获取百度Access Token失败，响应未包含access_token");
            return null;
        } catch (Exception e) {
            logger.error("获取百度Access Token异常: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    @Override
    public String textToSpeech(String text, int speechRate) {
        if (text == null || text.trim().isEmpty()) return null;
        if (!hasText(accessToken)) {
            accessToken = getAccessToken();
            if (!hasText(accessToken)) return null;
        }
        try {
            StringBuilder params = new StringBuilder();
            params.append("tok=").append(accessToken);
            params.append("&tex=").append(java.net.URLEncoder.encode(text, "UTF-8"));
            params.append("&per=").append(ttsConfig.getPer());
            params.append("&spd=").append(speechRate);
            params.append("&pit=").append(ttsConfig.getPitch());
            params.append("&vol=").append(ttsConfig.getVolume());
            params.append("&aue=").append(ttsConfig.getAue());
            params.append("&cuid=").append(ttsConfig.getAppId());
            params.append("&lan=zh&ctp=1");
            byte[] audioData = restTemplate.getForObject(ttsConfig.getTtsUrl() + "?" + params, byte[].class);
            if (audioData == null || audioData.length == 0) return null;
            String responseStr = new String(audioData);
            if (responseStr.startsWith("{")) {
                JsonNode errorNode = objectMapper.readTree(responseStr);
                if (errorNode.has("err_msg")) return null;
            }
            return "data:audio/mp3;base64," + Base64.getEncoder().encodeToString(audioData);
        } catch (Exception e) {
            logger.error("文本转语音异常: {}", e.getClass().getSimpleName());
            if (e.getMessage() != null && e.getMessage().contains("access_token")) accessToken = getAccessToken();
            return null;
        }
    }

    @Override
    public MultipartFile textToSpeechFile(String text, int speechRate) {
        String base64Audio = textToSpeech(text, speechRate);
        if (base64Audio == null) return null;
        try {
            byte[] audioBytes = Base64.getDecoder().decode(base64Audio.replace("data:audio/mp3;base64,", ""));
            Path tempFile = Files.createTempFile("tts_", ".mp3");
            Files.write(tempFile, audioBytes);
            return new TempMultipartFile(tempFile.toFile(), "audio/mp3", "speech.mp3");
        } catch (Exception e) {
            logger.error("创建音频文件失败: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }

    private static class TempMultipartFile implements MultipartFile {
        private final File file; private final String contentType; private final String originalFilename;
        TempMultipartFile(File file, String contentType, String originalFilename) {
            this.file = file; this.contentType = contentType; this.originalFilename = originalFilename;
        }
        public String getName() { return "file"; }
        public String getOriginalFilename() { return originalFilename; }
        public String getContentType() { return contentType; }
        public boolean isEmpty() { return file.length() == 0; }
        public long getSize() { return file.length(); }
        public byte[] getBytes() throws IOException { return Files.readAllBytes(file.toPath()); }
        public InputStream getInputStream() throws IOException { return new FileInputStream(file); }
        public void transferTo(File dest) throws IOException, IllegalStateException { Files.copy(file.toPath(), dest.toPath()); }
    }
}
