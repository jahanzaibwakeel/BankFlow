package com.bankflow.api.dto;

import com.bankflow.api.domain.AccountStatus;
import com.bankflow.api.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AccountDtos {
    private AccountDtos() {
    }

    public record CreateAccountRequest(@NotNull AccountType type) {
    }

    public record MoneyRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        String description
    ) {
    }

    public record AccountResponse(UUID id, String accountNumber, AccountType type, AccountStatus status, BigDecimal balance, Instant createdAt) {
    }
}
