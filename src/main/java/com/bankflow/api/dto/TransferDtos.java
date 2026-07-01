package com.bankflow.api.dto;

import com.bankflow.api.domain.TransferStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransferDtos {
    private TransferDtos() {
    }

    public record TransferRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationAccountId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        String description
    ) {
    }

    public record ReviewRequest(@NotNull TransferStatus status, String note) {
    }

    public record TransferResponse(UUID id, UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String description, TransferStatus status, Instant createdAt) {
    }
}
