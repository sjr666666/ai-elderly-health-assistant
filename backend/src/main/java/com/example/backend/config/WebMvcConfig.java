package com.example.backend.config;

import com.example.backend.common.ActiveTimeInterceptor;
import com.example.backend.common.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final ActiveTimeInterceptor activeTimeInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor, ActiveTimeInterceptor activeTimeInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.activeTimeInterceptor = activeTimeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册限流拦截器，只对AI相关接口生效
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/emergency/**")
                .addPathPatterns("/api/ai/**");

        // 注册活跃时间拦截器，对老人端关键接口生效
        registry.addInterceptor(activeTimeInterceptor)
                .addPathPatterns("/api/v1/plan/**")
                .addPathPatterns("/api/v1/box/**")
                .addPathPatterns("/api/emergency/**")
                .addPathPatterns("/api/v1/daily-lesson/**")
                .addPathPatterns("/api/v1/drug/recognize/**")
                .addPathPatterns("/api/ai/**")
                .addPathPatterns("/api/v1/drug/**")
                .addPathPatterns("/api/conflict/**");
    }
}