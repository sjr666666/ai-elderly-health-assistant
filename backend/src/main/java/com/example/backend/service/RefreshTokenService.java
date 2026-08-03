package com.example.backend.service;

import com.example.backend.config.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final String PREFIX = "auth:refresh:";
    private final StringRedisTemplate redis;
    private final JwtUtils jwtUtils;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration:2592000000}")
    private long refreshExpiration;

    public String issue(Long userId, String username, String role) {
        String raw = randomToken();
        Map<String, String> session = new HashMap<>();
        session.put("userId", String.valueOf(userId));
        session.put("username", username);
        session.put("role", role);
        redis.opsForHash().putAll(key(raw), session);
        redis.expire(key(raw), Duration.ofMillis(refreshExpiration));
        return raw;
    }

    public Map<String, String> consumeAndRotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("刷新凭证无效或已失效");
        String oldKey = key(rawToken);
        Map<Object, Object> session = redis.opsForHash().entries(oldKey);
        if (session.isEmpty()) throw new IllegalArgumentException("刷新凭证无效或已失效");
        redis.delete(oldKey);
        Long userId = Long.valueOf(String.valueOf(session.get("userId")));
        String username = String.valueOf(session.get("username"));
        String role = String.valueOf(session.get("role"));
        Map<String, String> result = new HashMap<>();
        result.put("accessToken", jwtUtils.generateToken(userId, username, role));
        result.put("refreshToken", issue(userId, username, role));
        return result;
    }

    public void revoke(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) redis.delete(key(rawToken));
    }

    public void revokeAllForUser(Long userId) {
        redis.keys(PREFIX + "*").forEach(sessionKey -> {
            Object storedUserId = redis.opsForHash().get(sessionKey, "userId");
            if (String.valueOf(userId).equals(String.valueOf(storedUserId))) redis.delete(sessionKey);
        });
    }

    private String key(String raw) { return PREFIX + hash(raw); }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : digest) result.append(String.format("%02x", b));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("无法生成刷新凭证摘要", e);
        }
    }
}
