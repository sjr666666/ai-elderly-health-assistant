package com.example.backend.websocket;

import com.example.backend.config.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Authenticates WebSocket handshakes before a session is accepted.
 *
 * Token 获取顺序（浏览器 WebSocket 无法自定义请求头，只能通过 URL 查询参数携带）：
 * 1. URL 查询参数 token（前端浏览器主用方式）
 * 2. Authorization: Bearer 请求头（工具客户端备用方式）
 */
@Slf4j
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

        String token = resolveToken(servletRequest);
        if (token == null) {
            log.warn("WebSocket handshake rejected: missing token");
            return false;
        }
        if (!jwtUtils.validateToken(token)) {
            log.warn("WebSocket handshake rejected: invalid or expired token");
            return false;
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        String role = jwtUtils.getRoleFromToken(token);
        if (userId == null || role == null) {
            log.warn("WebSocket handshake rejected: token lacks userId/role");
            return false;
        }
        attributes.put("userId", userId);
        attributes.put("role", role);
        return true;
    }

    /**
     * 解析 token：优先 URL 查询参数（浏览器 WebSocket 场景），其次 Authorization 请求头。
     */
    private String resolveToken(HttpServletRequest servletRequest) {
        // 1. URL 查询参数 token
        String queryToken = servletRequest.getParameter("token");
        if (StringUtils.hasText(queryToken)) {
            return queryToken.trim();
        }
        // 2. Authorization: Bearer 请求头
        String authorization = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Do not log tokens or other handshake credentials.
    }
}
