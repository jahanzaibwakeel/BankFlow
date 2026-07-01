package com.bankflow.api.config;

public interface RateLimiter {
    boolean tryConsume(String key);
}
