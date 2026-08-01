package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
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
            String current = redisTemplate.opsForValue().get(key);
            if (token.equals(current)) {
                redisTemplate.delete(key);
            }
        }
    }
}
