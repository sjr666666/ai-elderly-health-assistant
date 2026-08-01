package com.example.backend.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Per-user/IP fixed-window limiter for expensive AI endpoints. */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String userId = getAuthenticatedUserId();
        String key = userId != null ? "user:" + userId : "ip:" + getClientIp(request);
        RateLimitInfo info = requestCounts.computeIfAbsent(key, ignored -> new RateLimitInfo());

        synchronized (info) {
            long now = System.currentTimeMillis();
            if (now - info.lastResetTime >= 60_000L) {
                info.count.set(0);
                info.lastResetTime = now;
            }
            if (info.count.get() >= MAX_REQUESTS_PER_MINUTE) {
                logger.warn("Rate limit exceeded: key={}, uri={}", key, request.getRequestURI());
                sendRateLimitResponse(response);
                return false;
            }
            info.count.incrementAndGet();
        }
        return true;
    }

    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            return null;
        }
        return String.valueOf(authentication.getPrincipal());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",", 2)[0].trim() : ip;
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ResponseResult<String> result = ResponseResult.fail(
                ResponseCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private static class RateLimitInfo {
        private final AtomicInteger count = new AtomicInteger();
        private volatile long lastResetTime = System.currentTimeMillis();
    }
}
