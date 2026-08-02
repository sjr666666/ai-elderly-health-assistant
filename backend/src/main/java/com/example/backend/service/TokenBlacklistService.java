package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * JWT Access Token 黑名单服务
 * 登出时将 token 的 jti 写入 Redis，TTL 与 token 剩余有效期一致，
 * 使已登出的 access token 立即失效，无需等待自然过期。
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redis;

    /**
     * 将指定 token 加入黑名单（按剩余有效期设置 TTL，超时自动清理）
     */
    public void blacklist(String jti, Duration ttl) {
        if (jti == null || jti.isBlank()) return;
        redis.opsForValue().set(PREFIX + jti, "1", ttl);
    }

    /**
     * 校验 token 是否已在黑名单
     */
    public boolean isBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
