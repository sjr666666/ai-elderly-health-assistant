package com.example.backend.task;

import com.example.backend.config.ScheduledLock.DistributedTaskLock;
import com.example.backend.websocket.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxTask {
    private final JdbcTemplate jdbcTemplate;
    private final WebSocketHandler webSocketHandler;
    private final DistributedTaskLock lock;

    @Scheduled(fixedDelay = 3000)
    public void dispatch() {
        String token = lock.tryAcquire("notification-outbox", Duration.ofSeconds(20));
        if (token == null) return;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, user_id, payload FROM notification_outbox WHERE status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= NOW()) ORDER BY id LIMIT 50");
            for (Map<String, Object> row : rows) {
                long id = ((Number) row.get("id")).longValue();
                try {
                    webSocketHandler.sendMessageToUser(((Number) row.get("user_id")).longValue(), String.valueOf(row.get("payload")));
                    jdbcTemplate.update("UPDATE notification_outbox SET status='SENT', sent_at=NOW(), updated_at=NOW() WHERE id=? AND status='PENDING'", id);
                } catch (Exception error) {
                    jdbcTemplate.update("UPDATE notification_outbox SET attempts=attempts+1, next_retry_at=DATE_ADD(NOW(), INTERVAL LEAST(attempts + 1, 10) MINUTE), updated_at=NOW() WHERE id=?", id);
                    log.warn("通知 Outbox 发送失败，稍后重试，id={}", id, error);
                }
            }
        } finally {
            lock.release("notification-outbox", token);
        }
    }
}
