package com.example.backend.config;

import com.example.backend.common.ResponseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

/**
 * Spring Security配置类 - 配置JWT认证和API访问规则
 *
 * 认证规则：
 * - 登录/注册接口：无需认证
 * - 家属端API（/api/v1/guardian/**）：需要ROLE_FAMILY角色
 * - 老人端API（/api/v1/**）：需要ROLE_ELDER或ROLE_FAMILY角色
 * - 其他API：默认需要认证（白名单策略）
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（使用JWT不需要CSRF）
            .csrf().disable()
            // 无状态会话（不使用Session）
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                // CORS 预检请求放行
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                // 登录和注册接口无需认证
                .antMatchers(HttpMethod.POST, "/api/v1/user/login", "/api/v1/user/register", "/api/v1/user/refresh", "/api/v1/user/logout").permitAll()
                // 家属端API需要FAMILY角色认证
                .antMatchers("/api/v1/guardian/**").hasRole("FAMILY")
                // 老人端核心API需要认证（ELDER或FAMILY角色均可）
                .antMatchers("/api/v1/plan/**").hasAnyRole("ELDER", "FAMILY")
                .antMatchers("/api/v1/elder/**").hasAnyRole("ELDER", "FAMILY")
                .antMatchers("/api/v1/daily-lesson/**").hasAnyRole("ELDER", "FAMILY")
                .antMatchers("/api/v1/drug/**").hasAnyRole("ELDER", "FAMILY")
                .antMatchers("/api/v1/box/**").hasAnyRole("ELDER", "FAMILY")
                .antMatchers("/api/v1/medicine/**").hasAnyRole("ELDER", "FAMILY")
                // 用户个人信息接口需要认证
                .antMatchers("/api/v1/user/profile").hasAnyRole("ELDER", "FAMILY")
                .antMatchers("/api/v1/user/password").hasAnyRole("ELDER", "FAMILY")
                // AI对话、药物冲突检测、紧急事件需要认证
                .antMatchers("/api/ai/**").hasAnyRole("ELDER", "FAMILY")
                .antMatchers("/api/conflict/**").hasAnyRole("ELDER", "FAMILY")
                .antMatchers("/api/emergency/**").hasAnyRole("ELDER", "FAMILY")
                .antMatchers("/api/weekly-report/**").hasAnyRole("ELDER", "FAMILY")
                // 其他所有请求都需要认证（白名单策略）
                .anyRequest().authenticated()
            .and()
            // 统一处理认证失败（401）和授权失败（403），返回JSON格式响应
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ResponseResult<Void> result = ResponseResult.fail(401, "未认证或认证已过期，请重新登录");
                    response.getWriter().write(objectMapper.writeValueAsString(result));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ResponseResult<Void> result = ResponseResult.fail(403, "无权访问该资源");
                    response.getWriter().write(objectMapper.writeValueAsString(result));
                })
            .and()
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers
                .contentTypeOptions()
                .and()
                .frameOptions().deny());

        return http.build();
    }
}
