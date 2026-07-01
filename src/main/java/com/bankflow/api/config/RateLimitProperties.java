package com.bankflow.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bankflow.rate-limit")
public record RateLimitProperties(
    int capacity,
    int refillPerMinute,
    Backend backend,
    String redisKeyPrefix,
    boolean redisFailOpen
) {
    public RateLimitProperties {
        if (capacity <= 0) {
            capacity = 120;
        }
        if (refillPerMinute <= 0) {
            refillPerMinute = capacity;
        }
        if (backend == null) {
            backend = Backend.MEMORY;
        }
        if (redisKeyPrefix == null || redisKeyPrefix.isBlank()) {
            redisKeyPrefix = "bankflow:rate-limit";
        }
    }

    public enum Backend {
        MEMORY,
        REDIS
    }
}
