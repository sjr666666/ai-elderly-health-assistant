package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisOcrTaskQueue {
    public static final String STREAM = "tasks:ocr";
    public static final String GROUP = "ocr-workers";
    private final StringRedisTemplate redis;

    public RecordId publish(Long recordId) {
        RecordId id = redis.opsForStream().add(MapRecord.create(STREAM, Map.of("recordId", String.valueOf(recordId))));
        try {
            redis.opsForStream().createGroup(STREAM, org.springframework.data.redis.connection.stream.ReadOffset.from("0-0"), GROUP);
        } catch (Exception ignored) {
            // The group is created by another worker or already exists.
        }
        return id;
    }
}
