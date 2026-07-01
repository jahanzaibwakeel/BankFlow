package com.bankflow.api.dto;

import com.bankflow.api.domain.AuditAction;
import java.time.Instant;
import java.util.UUID;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record AuditLogResponse(UUID id, String actorUserId, AuditAction action, String resourceType, String resourceId, String details, Instant createdAt) {
    }
}
