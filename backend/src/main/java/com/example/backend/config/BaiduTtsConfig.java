package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度语音合成（TTS）配置类
 */
@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "baidu.tts")
public class BaiduTtsConfig {

    private String appId;

    private String apiKey;

    private String secretKey;

    /**
     * 百度access token获取地址
     */
    private static final String ACCESS_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    /**
     * 百度语音合成API地址
     */
    private static final String TTS_URL = "https://tsn.baidu.com/text2audio";

    /**
     * 语速，取值0-15，默认为5
     */
    private int speed = 5;

    /**
     * 音调，取值0-15，默认为5
     */
    private int pitch = 5;

    /**
     * 音量，取值0-15，默认为5
     */
    private int volume = 5;

    /**
     * 人物ID，0为女声，1为男声，3为情感男声，4为情感女声
     */
    private int per = 3;

    /**
     * 采样率，固定值16000
     */
    private int aue = 6;

    public String getAccessTokenUrl() {
        return ACCESS_TOKEN_URL;
    }

    public String getTtsUrl() {
        return TTS_URL;
    }
}
