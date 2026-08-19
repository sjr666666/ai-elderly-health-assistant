package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOcrTaskQueue {
    public static final String STREAM = "tasks:ocr";
    public static final String GROUP = "ocr-workers";
    /** 死信流：重试超过上限仍失败的消息写入此处，便于后续人工/监控排查 */
    public static final String DEAD_LETTER_STREAM = "tasks:ocr:dead";

    private final StringRedisTemplate redis;

    /**
     * 认领 idle 超时的 pending 消息：
     * 1) XPENDING 拿到该 group 下的 pending 消息 ID 列表；
     * 2) 用 XCLAIM 借 MINIDLE 语义，只认领已经长期（超过 minIdleMs）未被消费/确认的旧消息。
     * 之所以不用 XAUTOCLAIM：spring-data-redis 2.7 无 autoclaim()，且 XAUTOCLAIM 返回不含消息 ID，
     * 认领后无法对其 XACK；XCLAIM 能一并返回消息 ID，功能等价。
     */
    private static final String CLAIM_SCRIPT =
            "local pending = redis.call('XPENDING', KEYS[1], ARGV[1], '-', '+', ARGV[4]) " +
                    "local ids = {} " +
                    "for _, item in ipairs(pending) do ids[#ids + 1] = item[1] end " +
                    "if #ids == 0 then return {} end " +
                    "return redis.call('XCLAIM', KEYS[1], ARGV[1], ARGV[2], ARGV[3], unpack(ids))";

    /** 认领到的消息：entry id + 具体字段 */
    public static final class ClaimedTask {
        public final RecordId id;
        public final Map<String, String> fields;

        public ClaimedTask(RecordId id, Map<String, String> fields) {
            this.id = id;
            this.fields = fields;
        }
    }

    public RecordId publish(Long recordId) {
        RecordId id = redis.opsForStream().add(MapRecord.create(STREAM,
                Map.of("recordId", String.valueOf(recordId), "retryCount", "0")));
        ensureGroup();
        return id;
    }

    /**
     * 认领 idle 超过 minIdleSeconds 秒的 pending 消息并返回，交给消费者重新处理。
     */
    public List<ClaimedTask> claimIdle(String consumer, long minIdleSeconds, int count) {
        List<Object> result = redis.execute(new DefaultRedisScript<>(CLAIM_SCRIPT, List.class),
                List.of(STREAM), GROUP, consumer, String.valueOf(minIdleSeconds * 1000L), String.valueOf(count));
        List<ClaimedTask> tasks = new ArrayList<>();
        if (result == null) {
            return tasks;
        }
        for (Object item : result) {
            // 每条 = [ id, [f1,v1, f2,v2, ...] ]
            List<Object> entry = asList(item);
            if (entry == null || entry.size() < 2) {
                continue;
            }
            RecordId id = RecordId.of(toText(entry.get(0)));
            Map<String, String> fields = new LinkedHashMap<>();
            List<Object> kv = asList(entry.get(1));
            if (kv != null) {
                for (int i = 0; i + 1 < kv.size(); i += 2) {
                    fields.put(toText(kv.get(i)), toText(kv.get(i + 1)));
                }
            }
            tasks.add(new ClaimedTask(id, fields));
        }
        return tasks;
    }

    public void acknowledge(RecordId id) {
        redis.opsForStream().acknowledge(GROUP, STREAM, id);
    }

    /** 失败重投：从队尾 XADD 一条携带更高 retryCount 的新消息 */
    public void retry(Long recordId, int retryCount) {
        ensureGroup();
        redis.opsForStream().add(MapRecord.create(STREAM,
                Map.of("recordId", String.valueOf(recordId), "retryCount", String.valueOf(retryCount))));
    }

    /** 重试超限：写入死信流，供人工/监控处理 */
    public void toDeadLetter(Long recordId, int retryCount) {
        redis.opsForStream().add(MapRecord.create(DEAD_LETTER_STREAM,
                Map.of("recordId", String.valueOf(recordId), "retryCount", String.valueOf(retryCount))));
        log.warn("OCR task exceeded max retries, moved to dead letter, recordId={}, retryCount={}", recordId, retryCount);
    }

    private void ensureGroup() {
        try {
            redis.opsForStream().createGroup(STREAM, org.springframework.data.redis.connection.stream.ReadOffset.from("0-0"), GROUP);
        } catch (Exception ignored) {
            // The group is created by another worker or already exists.
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return null;
    }

    private static String toText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return String.valueOf(value);
    }
}