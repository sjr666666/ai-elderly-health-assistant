package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void enqueue(String eventType, Long notificationId, Long userId, String payload) {
        jdbcTemplate.update("INSERT IGNORE INTO notification_outbox (event_type, aggregate_id, user_id, payload, status, next_retry_at) VALUES (?, ?, ?, ?, 'PENDING', NOW())", eventType, notificationId, userId, payload);
    }
}
