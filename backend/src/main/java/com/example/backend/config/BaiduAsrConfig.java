package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度语音识别（ASR）配置类
 * 与百度 TTS 共用同一平台 API Key / Secret Key（百度智能云统一鉴权）
 */
@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "baidu.asr")
public class BaiduAsrConfig {

    /** 百度 API Key（获取 access_token 用） */
    private String apiKey;

    /** 百度 Secret Key（获取 access_token 用） */
    private String secretKey;

    /**
     * 百度 access token 获取地址（与 TTS 共用）
     */
    private static final String ACCESS_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    /**
     * 百度短语音识别 API 地址
     */
    private static final String ASR_URL = "https://aip.baidubce.com/rpc/2.0/aasr/v1/recognize";

    /** 识别音频格式（wav / pcm），默认 wav */
    private String format = "wav";

    /** 采样率（16000 / 8000），默认 16000 */
    private int rate = 16000;

    public String getAccessTokenUrl() {
        return ACCESS_TOKEN_URL;
    }

    public String getAsrUrl() {
        return ASR_URL;
    }
}
