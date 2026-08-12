package com.example.backend.config;

import com.example.backend.common.ActiveTimeInterceptor;
import com.example.backend.common.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final ActiveTimeInterceptor activeTimeInterceptor;
    private final DevelopmentOnlyEndpointFilter developmentOnlyEndpointFilter;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor, ActiveTimeInterceptor activeTimeInterceptor,
                        DevelopmentOnlyEndpointFilter developmentOnlyEndpointFilter) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.activeTimeInterceptor = activeTimeInterceptor;
        this.developmentOnlyEndpointFilter = developmentOnlyEndpointFilter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册限流拦截器：AI 接口（昂贵）+ 注册/登录/刷新（防撞库）+ OCR 上传（防刷）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/emergency/**")
                .addPathPatterns("/api/ai/**")
                .addPathPatterns("/api/v1/user/register")
                .addPathPatterns("/api/v1/user/login")
                .addPathPatterns("/api/v1/user/refresh")
                .addPathPatterns("/api/v1/drug/recognize/**");

        // 注册活跃时间拦截器，对老人端和家属端关键接口生效
        registry.addInterceptor(activeTimeInterceptor)
                .addPathPatterns("/api/v1/plan/**")
                .addPathPatterns("/api/v1/box/**")
                .addPathPatterns("/api/emergency/**")
                .addPathPatterns("/api/v1/daily-lesson/**")
                .addPathPatterns("/api/v1/drug/recognize/**")
                .addPathPatterns("/api/ai/**")
                .addPathPatterns("/api/v1/drug/**")
                .addPathPatterns("/api/conflict/**")
                .addPathPatterns("/api/v1/guardian/**");

        registry.addInterceptor(developmentOnlyEndpointFilter)
                .addPathPatterns("/**/test/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Uploaded files are never served as public static resources.
    }
}
