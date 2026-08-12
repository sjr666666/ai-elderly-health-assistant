package com.example.backend.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Per-user/IP fixed-window limiter for expensive / abuse-prone endpoints. */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    /** 按路径前缀覆盖限额（次/分钟）；未匹配使用默认值。OCR 拍照识别允许更频繁，避免卡住真实老人用户。 */
    private static final Map<String, Integer> PATH_LIMITS = Map.of(
            "/api/v1/drug/recognize/", 20
    );

    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RateLimitInterceptor(ObjectMapper objectMapper, StringRedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    /** Kept for focused unit tests that do not load a Redis context. */
    public RateLimitInterceptor(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String userId = getAuthenticatedUserId();
        String key = userId != null ? "user:" + userId : "ip:" + getClientIp(request);
        int limit = resolveLimit(request.getRequestURI());
        if (redisTemplate != null) {
            try {
                String redisKey = "rate-limit:" + key;
                Long count = redisTemplate.opsForValue().increment(redisKey);
                if (count != null && count == 1L) {
                    redisTemplate.expire(redisKey, java.time.Duration.ofMinutes(1));
                }
                if (count != null && count > limit) {
                    logger.warn("Rate limit exceeded: key={}, uri={}", key, request.getRequestURI());
                    sendRateLimitResponse(response);
                    return false;
                }
                return true;
            } catch (RuntimeException exception) {
                logger.warn("Redis rate limiter unavailable; falling back to local limiter: {}",
                        exception.getClass().getSimpleName());
            }
        }

        RateLimitInfo info = requestCounts.computeIfAbsent(key, ignored -> new RateLimitInfo());

        synchronized (info) {
            long now = System.currentTimeMillis();
            if (now - info.lastResetTime >= 60_000L) {
                info.count.set(0);
                info.lastResetTime = now;
            }
            if (info.count.get() >= limit) {
                logger.warn("Rate limit exceeded: key={}, uri={}", key, request.getRequestURI());
                sendRateLimitResponse(response);
                return false;
            }
            info.count.incrementAndGet();
        }
        return true;
    }

    private int resolveLimit(String uri) {
        for (Map.Entry<String, Integer> entry : PATH_LIMITS.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return MAX_REQUESTS_PER_MINUTE;
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
