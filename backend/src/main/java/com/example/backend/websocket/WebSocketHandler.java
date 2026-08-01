package com.example.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Pushes notifications to sessions identified by an authenticated user ID. */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = authenticatedUserId(session);
        if (userId == null) {
            return;
        }
        sessions.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        log.info("WebSocket connection established - userId: {}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = authenticatedUserId(session);
        if (userId == null) {
            return;
        }
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session.getId());
            if (userSessions.isEmpty()) {
                sessions.remove(userId, userSessions);
            }
        }
        log.info("WebSocket connection closed - userId: {}, status: {}", userId, status.getCode());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket transport error - userId: {}, type: {}", authenticatedUserId(session),
                exception.getClass().getSimpleName());
        if (session.isOpen()) {
            session.close();
        }
    }

    public void sendMessageToUser(Long userId, String message) {
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        for (WebSocketSession session : userSessions.values()) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException exception) {
                log.warn("WebSocket send failed - userId: {}, type: {}", userId,
                        exception.getClass().getSimpleName());
            }
        }
    }

    public boolean isUserOnline(Long userId) {
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        return userSessions != null && userSessions.values().stream().anyMatch(WebSocketSession::isOpen);
    }

    private Long authenticatedUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId instanceof Long ? (Long) userId : null;
    }
}
