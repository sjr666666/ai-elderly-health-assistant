package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrStreamConsumer {
    private final StringRedisTemplate redis;
    private final RedisOcrTaskQueue queue;
    private final OcrAsyncService ocrAsyncService;
    private final Executor taskExecutor;

    @PostConstruct
    public void start() {
        taskExecutor.execute(this::consume);
    }

    private void consume() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    redis.opsForStream().createGroup(RedisOcrTaskQueue.STREAM, ReadOffset.from("0-0"), RedisOcrTaskQueue.GROUP);
                } catch (Exception ignored) {
                    // The stream may not exist until the first upload, or the group may already exist.
                }
                List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                        Consumer.from(RedisOcrTaskQueue.GROUP, "worker-" + System.identityHashCode(this)),
                        org.springframework.data.redis.connection.stream.StreamReadOptions.empty().count(1).block(Duration.ofSeconds(5)),
                        StreamOffset.create(RedisOcrTaskQueue.STREAM, ReadOffset.lastConsumed()));
                if (records == null) continue;
                for (MapRecord<String, Object, Object> record : records) {
                    Object id = record.getValue().get("recordId");
                    try {
                        ocrAsyncService.processOcrAsync(Long.valueOf(String.valueOf(id)));
                        redis.opsForStream().acknowledge(RedisOcrTaskQueue.GROUP, record);
                    } catch (Exception error) {
                        log.error("OCR stream task failed, recordId={}", id, error);
                    }
                }
            }
        } catch (Exception error) {
            log.error("OCR stream consumer stopped", error);
        }
    }
}
