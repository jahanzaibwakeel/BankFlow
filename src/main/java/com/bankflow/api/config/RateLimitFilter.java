package com.bankflow.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final int refillPerMinute;

    public RateLimitFilter(@Value("${bankflow.rate-limit.capacity}") int capacity, @Value("${bankflow.rate-limit.refill-per-minute}") int refillPerMinute) {
        this.capacity = capacity;
        this.refillPerMinute = refillPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity, Instant.now()));
        if (!bucket.tryConsume(capacity, refillPerMinute)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"data\":null,\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\",\"fields\":{}},\"requestId\":\"" + (MDC.get("requestId") == null ? "" : MDC.get("requestId")) + "\",\"timestamp\":\"" + Instant.now() + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static final class Bucket {
        private int tokens;
        private Instant lastRefill;

        private Bucket(int tokens, Instant lastRefill) {
            this.tokens = tokens;
            this.lastRefill = lastRefill;
        }

        synchronized boolean tryConsume(int capacity, int refillPerMinute) {
            long elapsedSeconds = Math.max(0, Instant.now().getEpochSecond() - lastRefill.getEpochSecond());
            int refill = (int) (elapsedSeconds * refillPerMinute / 60);
            if (refill > 0) {
                tokens = Math.min(capacity, tokens + refill);
                lastRefill = Instant.now();
            }
            if (tokens <= 0) {
                return false;
            }
            tokens--;
            return true;
        }
    }
}
