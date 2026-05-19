package com.example.backend.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求限流拦截器
 * 限制用户每分钟最多发送10次请求
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    // 最大请求次数（每分钟）
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    // 存储用户请求计数：key为用户标识（IP或用户ID），value为请求计数和时间戳
    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = getUserId(request);
        
        // 如果无法获取用户ID，使用IP地址作为标识
        String key = userId != null ? userId : getClientIp(request);

        RateLimitInfo info = requestCounts.computeIfAbsent(key, k -> new RateLimitInfo());

        synchronized (info) {
            long currentTime = System.currentTimeMillis();
            long elapsedMinutes = (currentTime - info.lastResetTime) / 60000;

            // 如果超过1分钟，重置计数
            if (elapsedMinutes >= 1) {
                info.count.set(0);
                info.lastResetTime = currentTime;
            }

            if (info.count.get() >= MAX_REQUESTS_PER_MINUTE) {
                logger.warn("请求限流 - 用户: {}, IP: {}, 请求URL: {}", 
                        userId, getClientIp(request), request.getRequestURI());
                
                sendRateLimitResponse(response);
                return false;
            }

            info.count.incrementAndGet();
        }

        return true;
    }

    /**
     * 获取用户ID
     */
    private String getUserId(HttpServletRequest request) {
        // 尝试从请求头获取用户ID
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // 尝试从请求参数获取用户ID
        userId = request.getParameter("userId");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        return null;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果是多个代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 发送限流响应
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ResponseResult<String> result = ResponseResult.fail(
                ResponseCode.TOO_MANY_REQUESTS.getCode(),
                "请求过于频繁，请稍后再试"
        );

        response.getWriter().write(objectMapper.writeValueAsString(result));
            response.getWriter().flush();
            response.getWriter().close();
    }

    /**
     * 限流信息内部类
     */
    private static class RateLimitInfo {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long lastResetTime = System.currentTimeMillis();
    }
}