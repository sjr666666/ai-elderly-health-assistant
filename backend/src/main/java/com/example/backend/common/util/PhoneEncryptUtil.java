package com.example.backend.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 手机号AES加密工具
 * 用于敏感手机号加密存储与脱敏展示
 */
@Slf4j
public class PhoneEncryptUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * AES加密（手机号 → 密文Base64）
     *
     * @param plainPhone 明文手机号
     * @param key        AES密钥（16字节）
     * @return Base64编码的密文，加密失败返回原文
     */
    public static String encrypt(String plainPhone, String key) {
        if (plainPhone == null || plainPhone.isEmpty()) {
            return plainPhone;
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plainPhone.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("手机号AES加密失败", e);
            return plainPhone;
        }
    }

    /**
     * AES解密（密文Base64 → 明文手机号）
     *
     * @param encryptedPhone Base64编码的密文
     * @param key            AES密钥（16字节）
     * @return 明文手机号，解密失败返回原文（兼容历史明文数据）
     */
    public static String decrypt(String encryptedPhone, String key) {
        if (encryptedPhone == null || encryptedPhone.isEmpty()) {
            return encryptedPhone;
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedPhone);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败说明是历史明文数据，直接返回原文
            log.debug("手机号解密失败，可能为历史明文数据: {}", e.getMessage());
            return encryptedPhone;
        }
    }

    /**
     * 手机号脱敏：138****1234
     *
     * @param phone 明文手机号
     * @return 脱敏后的手机号
     */
    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        // 国内手机号11位：前3后4
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        // 其他长度：保留前3后2
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
}
