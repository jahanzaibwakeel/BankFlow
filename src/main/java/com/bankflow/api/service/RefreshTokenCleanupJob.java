package com.bankflow.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final AuthService authService;

    public RefreshTokenCleanupJob(AuthService authService) {
        this.authService = authService;
    }

    @Scheduled(cron = "${bankflow.jwt.refresh-token-cleanup-cron:0 15 3 * * *}")
    public void purgeExpiredTokens() {
        int deleted = authService.purgeExpiredRefreshTokens();
        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens", deleted);
        }
    }
}
