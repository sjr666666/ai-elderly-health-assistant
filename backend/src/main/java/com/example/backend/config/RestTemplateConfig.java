package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 */
@Configuration
public class RestTemplateConfig {

    @Value("${rest.connection-timeout:10000}")
    private int connectionTimeout;

    @Value("${rest.read-timeout:30000}")
    private int readTimeout;

    @Value("${ai.rest.connection-timeout:10000}")
    private int aiConnectionTimeout;

    @Value("${ai.rest.read-timeout:120000}")
    private int aiReadTimeout;

    @Value("${tts.rest.connection-timeout:10000}")
    private int ttsConnectionTimeout;

    @Value("${tts.rest.read-timeout:30000}")
    private int ttsReadTimeout;

    /**
     * 通用 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }

    /**
     * AI服务专用 RestTemplate（较长读取超时，适配大模型响应）
     */
    @Bean
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(aiConnectionTimeout);
        factory.setReadTimeout(aiReadTimeout);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }

    /**
     * TTS服务专用 RestTemplate
     */
    @Bean
    public RestTemplate ttsRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(ttsConnectionTimeout);
        factory.setReadTimeout(ttsReadTimeout);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }
}
