package com.example.backend.websocket;

import com.example.backend.config.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/** Authenticates WebSocket handshakes before a session is accepted. */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String authorization = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        String token = authorization.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return false;
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        String role = jwtUtils.getRoleFromToken(token);
        if (userId == null || role == null) {
            return false;
        }
        attributes.put("userId", userId);
        attributes.put("role", role);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Do not log tokens or other handshake credentials.
    }
}
