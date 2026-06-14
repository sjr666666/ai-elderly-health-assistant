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

/**
 * WebSocket处理器 - 老人端通知实时推送
 */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    // elderId -> WebSocketSession
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long elderId = extractElderId(session);
        if (elderId != null) {
            sessions.put(elderId, session);
            log.info("WebSocket连接建立 - elderId: {}", elderId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long elderId = extractElderId(session);
        if (elderId != null) {
            sessions.remove(elderId);
            log.info("WebSocket连接关闭 - elderId: {}, status: {}", elderId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long elderId = extractElderId(session);
        log.error("WebSocket传输错误 - elderId: {}", elderId, exception);
        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 向指定老人发送消息
     */
    public void sendMessageToUser(Long elderId, String message) {
        WebSocketSession session = sessions.get(elderId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("WebSocket发送消息失败 - elderId: {}", elderId, e);
            }
        }
    }

    /**
     * 检查老人是否在线
     */
    public boolean isUserOnline(Long elderId) {
        WebSocketSession session = sessions.get(elderId);
        return session != null && session.isOpen();
    }

    /**
     * 从session中提取elderId
     * 连接URL格式：ws://host/ws/notifications?elderId=xxx
     */
    private Long extractElderId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "elderId".equals(kv[0])) {
                    try {
                        return Long.valueOf(kv[1]);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
