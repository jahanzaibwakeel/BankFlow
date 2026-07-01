package com.bankflow.api.dto;

import com.bankflow.api.domain.LedgerEntryType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class LedgerDtos {
    private LedgerDtos() {
    }

    public record LedgerEntryResponse(UUID id, UUID accountId, UUID transferId, LedgerEntryType entryType, BigDecimal amount, BigDecimal balanceAfter, String reference, Instant createdAt) {
    }
}
