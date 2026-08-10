package com.example.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具类单元测试
 * 覆盖:令牌生成、claims 解析、合法性校验、篡改/过期防护
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private static final String SECRET = "test-secret-key-for-jwt-unit-test-0123456789abcdef";
    private static final long EXPIRATION_MS = 60_000L;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();
        setField("secret", SECRET);
        setField("expiration", EXPIRATION_MS);
        jwtUtils.init();
    }

    private void setField(String name, Object value) throws Exception {
        Field field = JwtUtils.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(jwtUtils, value);
    }

    @Test
    void generateToken_containsAllClaims() {
        String token = jwtUtils.generateToken(10001L, "laowang", "elder");

        assertNotNull(token);
        assertEquals("laowang", jwtUtils.getUsernameFromToken(token));
        assertEquals(10001L, jwtUtils.getUserIdFromToken(token));
        assertEquals("elder", jwtUtils.getRoleFromToken(token));
        assertNotNull(jwtUtils.getJtiFromToken(token));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtils.generateToken(1L, "zhangsan", "family");
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtils.generateToken(1L, "laowang", "elder");
        String tampered = token.substring(0, token.length() - 4) + "abcd";
        assertFalse(jwtUtils.validateToken(tampered));
    }

    @Test
    void validateToken_expiredToken_returnsFalse() throws Exception {
        // 用 1ms 有效期生成令牌,必然过期
        setField("expiration", 1L);
        jwtUtils.init();
        String token = jwtUtils.generateToken(1L, "laowang", "elder");
        try {
            Thread.sleep(5);
        } catch (InterruptedException ignored) {
        }
        assertFalse(jwtUtils.validateToken(token));
    }

    @Test
    void init_weakSecret_throws() throws Exception {
        JwtUtils weak = new JwtUtils();
        setField("secret", "short");
        setField("expiration", 1000L);
        assertThrows(IllegalStateException.class, weak::init);
    }

    @Test
    void getRemainingMillis_validToken_positive() {
        String token = jwtUtils.generateToken(1L, "laowang", "elder");
        long remaining = jwtUtils.getRemainingMillis(token);
        assertTrue(remaining > 0 && remaining <= EXPIRATION_MS);
    }
}
