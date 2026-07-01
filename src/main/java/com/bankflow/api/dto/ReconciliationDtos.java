package com.bankflow.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReconciliationDtos {
    private ReconciliationDtos() {
    }

    public record AccountReconciliationIssue(UUID accountId, BigDecimal accountBalance, BigDecimal ledgerBalance, BigDecimal difference) {
    }

    public record TransferReconciliationIssue(UUID transferId, BigDecimal debits, BigDecimal credits, long entryCount) {
    }

    public record ReconciliationReport(
        Instant generatedAt,
        boolean balanced,
        List<AccountReconciliationIssue> accountIssues,
        List<TransferReconciliationIssue> transferIssues
    ) {
    }
}
