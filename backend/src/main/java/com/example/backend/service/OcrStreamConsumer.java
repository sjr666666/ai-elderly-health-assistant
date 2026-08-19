package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrStreamConsumer {
    /** 单条消息允许的最大重试次数，超过则进入死信 */
    private static final int MAX_RETRY = 3;
    /** 兜底认领参数：pending 消息 idle 超过该秒数则视为"可能丢失"，重新认领消费 */
    private static final long CLAIM_IDLE_SECONDS = 60;
    /** 每次兜底认领的消息条数上限 */
    private static final int CLAIM_COUNT = 20;

    private final StringRedisTemplate redis;
    private final RedisOcrTaskQueue queue;
    private final OcrAsyncService ocrAsyncService;
    private final Executor taskExecutor;

    @PostConstruct
    public void start() {
        taskExecutor.execute(this::consume);
    }

    private void consume() {
        String consumerName = "worker-" + System.identityHashCode(this);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    redis.opsForStream().createGroup(RedisOcrTaskQueue.STREAM, ReadOffset.from("0-0"), RedisOcrTaskQueue.GROUP);
                } catch (Exception ignored) {
                    // The stream may not exist until the first upload, or the group may already exist.
                }

                // 1) 读取并消费新消息
                List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                        Consumer.from(RedisOcrTaskQueue.GROUP, consumerName),
                        org.springframework.data.redis.connection.stream.StreamReadOptions.empty().count(1).block(Duration.ofSeconds(5)),
                        StreamOffset.create(RedisOcrTaskQueue.STREAM, ReadOffset.lastConsumed()));
                if (records != null) {
                    for (MapRecord<String, Object, Object> record : records) {
                        Map<String, String> fields = new HashMap<>();
                        record.getValue().forEach((k, v) -> {
                            Object val = v;
                            fields.put(String.valueOf(k), val == null ? null : val.toString());
                        });
                        handle(fields, record.getId());
                    }
                }

                // 2) PEL 兜底：认领 idle 过久、从未被确认（ack）的 pending 消息，重新消费
                try {
                    List<RedisOcrTaskQueue.ClaimedTask> stale = queue.claimIdle(consumerName, CLAIM_IDLE_SECONDS, CLAIM_COUNT);
                    for (RedisOcrTaskQueue.ClaimedTask task : stale) {
                        handle(task.fields, task.id);
                    }
                } catch (Exception error) {
                    log.error("OCR stream claim stale pending messages failed", error);
                }
            }
        } catch (Exception error) {
            log.error("OCR stream consumer stopped", error);
        }
    }

    /**
     * 处理单条消息：成功 ack；失败则 ack 并重投（retryCount+1），超过上限则 ack 并进入死信。
     * 先把消息 ack（移出 PEL）再通过 XADD 重投/入死信，避免旧的 pending 被反复认领。
     */
    private void handle(Map<String, String> fields, RecordId id) {
        String recordIdValue = fields.get("recordId");
        if (recordIdValue == null) {
            queue.acknowledge(id);
            return;
        }
        Long recordId = Long.valueOf(String.valueOf(recordIdValue));
        int retryCount = parseInt(fields.get("retryCount"));

        boolean ok;
        try {
            ok = ocrAsyncService.processOcrAsync(recordId);
        } catch (Exception error) {
            log.error("OCR stream task threw, recordId={}, retryCount={}", recordId, retryCount, error);
            ok = false;
        }

        if (ok) {
            queue.acknowledge(id);
            return;
        }

        queue.acknowledge(id);
        if (retryCount >= MAX_RETRY) {
            queue.toDeadLetter(recordId, retryCount);
        } else {
            log.warn("OCR stream task failed, retrying, recordId={}, retryCount={}", recordId, retryCount + 1);
            queue.retry(recordId, retryCount + 1);
        }
    }

    private int parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}