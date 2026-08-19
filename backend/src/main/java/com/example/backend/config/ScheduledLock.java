package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/** Small Redis lock helper used to prevent duplicate scheduled work across instances. */
@Configuration
public class ScheduledLock {

    @Bean
    public DistributedTaskLock distributedTaskLock(StringRedisTemplate redisTemplate) {
        return new DistributedTaskLock(redisTemplate);
    }

    public static final class DistributedTaskLock {
        private final StringRedisTemplate redisTemplate;

        /** 原子释放锁：仅当 key 的当前值等于持有者 token 时才删除，避免误删他人锁 */
        private static final String RELEASE_SCRIPT =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else " +
                        "return 0 " +
                        "end";

        private DistributedTaskLock(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        public String tryAcquire(String name, Duration ttl) {
            String token = UUID.randomUUID().toString();
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent("lock:scheduled:" + name, token, ttl);
            return Boolean.TRUE.equals(acquired) ? token : null;
        }

        public void release(String name, String token) {
            if (token == null) {
                return;
            }
            String key = "lock:scheduled:" + name;
            // 校验与删除在同一 Lua 脚本内原子执行，避免"校验通过后锁过期被他人抢占再误删"的竞态
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
            redisTemplate.execute(script, Collections.singletonList(key), token);
        }
    }
}
