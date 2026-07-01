package com.bankflow.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String userId;

    @Column(nullable = false, length = 160)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false, length = 64)
    private String operation;

    @Column(length = 80)
    private String resourceId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected IdempotencyKey() {
    }

    public IdempotencyKey(String userId, String idempotencyKey, String requestHash, String operation) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.operation = operation;
    }

    public void complete(UUID resourceId) {
        this.resourceId = resourceId.toString();
    }

    public String getUserId() { return userId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getOperation() { return operation; }
    public String getResourceId() { return resourceId; }
}
