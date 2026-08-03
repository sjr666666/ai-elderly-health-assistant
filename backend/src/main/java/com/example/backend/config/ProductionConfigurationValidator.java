package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

/** Fails fast when production uses demo credentials or unsafe defaults. */
@Configuration
@Profile("prod")
public class ProductionConfigurationValidator {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${phone.encrypt.key:}")
    private String phoneEncryptKey;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @PostConstruct
    public void validate() {
        requireStrongSecret("jwt.secret", jwtSecret, 32);
        requireStrongSecret("phone.encrypt.key", phoneEncryptKey, 16);
        if ("root".equalsIgnoreCase(datasourceUsername)) {
            throw new IllegalStateException("Production must use a least-privilege database account");
        }
        if (allowedOrigins.isBlank() || allowedOrigins.contains("*")) {
            throw new IllegalStateException("Production CORS origins must be explicitly configured");
        }
    }

    private void requireStrongSecret(String name, String value, int minLength) {
        if (value == null || value.isBlank() || value.length() < minLength
                || value.contains("change-me") || value.contains("local-dev")
                || value.contains("your_")) {
            throw new IllegalStateException(name + " must be a non-default secret of at least " + minLength + " characters");
        }
    }
}
