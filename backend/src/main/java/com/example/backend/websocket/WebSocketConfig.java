package com.example.backend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Value;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/notifications")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new));
    }
}
