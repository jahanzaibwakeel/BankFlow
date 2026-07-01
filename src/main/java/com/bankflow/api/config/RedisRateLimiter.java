package com.bankflow.api.config;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bankflow.rate-limit", name = "backend", havingValue = "redis")
public class RedisRateLimiter implements RateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final String SCRIPT = """
        local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
        local last_refill = tonumber(redis.call('HGET', KEYS[1], 'last_refill'))
        local now = tonumber(ARGV[1])
        local capacity = tonumber(ARGV[2])
        local refill_per_minute = tonumber(ARGV[3])
        local ttl_seconds = tonumber(ARGV[4])

        if tokens == nil then
          tokens = capacity
          last_refill = now
        end

        local elapsed = math.max(0, now - last_refill)
        local refill = math.floor(elapsed * refill_per_minute / 60)
        if refill > 0 then
          tokens = math.min(capacity, tokens + refill)
          last_refill = now
        end

        if tokens <= 0 then
          redis.call('HSET', KEYS[1], 'tokens', tokens, 'last_refill', last_refill)
          redis.call('EXPIRE', KEYS[1], ttl_seconds)
          return 0
        end

        tokens = tokens - 1
        redis.call('HSET', KEYS[1], 'tokens', tokens, 'last_refill', last_refill)
        redis.call('EXPIRE', KEYS[1], ttl_seconds)
        return 1
        """;

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final DefaultRedisScript<Long> script;

    public RedisRateLimiter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.script = new DefaultRedisScript<>(SCRIPT, Long.class);
    }

    @Override
    public boolean tryConsume(String key) {
        String redisKey = properties.redisKeyPrefix() + ":" + key;
        long now = System.currentTimeMillis() / 1000;
        long ttlSeconds = Math.max(60, Duration.ofMinutes(2).toSeconds());
        try {
            Long allowed = redisTemplate.execute(script, List.of(redisKey),
                String.valueOf(now),
                String.valueOf(properties.capacity()),
                String.valueOf(properties.refillPerMinute()),
                String.valueOf(ttlSeconds));
            return Long.valueOf(1).equals(allowed);
        } catch (RuntimeException ex) {
            if (properties.redisFailOpen()) {
                log.warn("Redis rate limiter unavailable; allowing request");
                return true;
            }
            throw ex;
        }
    }
}
