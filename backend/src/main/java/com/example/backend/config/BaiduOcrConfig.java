package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "baidu.ocr")
public class BaiduOcrConfig {

    private String appId;

    private String apiKey;

    private String secretKey;

    private static final String ACCESS_TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";

    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic";

    public String getAccessTokenUrl() {
        return ACCESS_TOKEN_URL;
    }

    public String getOcrUrl() {
        return OCR_URL;
    }
}