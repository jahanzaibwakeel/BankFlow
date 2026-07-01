package com.bankflow.api.config;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bankflow.rate-limit", name = "backend", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;

    public InMemoryRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(properties.capacity(), Instant.now()));
        return bucket.tryConsume(properties.capacity(), properties.refillPerMinute());
    }

    private static final class Bucket {
        private int tokens;
        private Instant lastRefill;

        private Bucket(int tokens, Instant lastRefill) {
            this.tokens = tokens;
            this.lastRefill = lastRefill;
        }

        synchronized boolean tryConsume(int capacity, int refillPerMinute) {
            Instant now = Instant.now();
            long elapsedSeconds = Math.max(0, now.getEpochSecond() - lastRefill.getEpochSecond());
            int refill = (int) (elapsedSeconds * refillPerMinute / 60);
            if (refill > 0) {
                tokens = Math.min(capacity, tokens + refill);
                lastRefill = now;
            }
            if (tokens <= 0) {
                return false;
            }
            tokens--;
            return true;
        }
    }
}
