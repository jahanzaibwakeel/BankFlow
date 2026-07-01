package com.bankflow.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bankflow.api.domain.Account;
import com.bankflow.api.domain.AccountStatus;
import com.bankflow.api.domain.AccountType;
import com.bankflow.api.domain.Role;
import com.bankflow.api.domain.User;
import com.bankflow.api.dto.AccountDtos.MoneyRequest;
import com.bankflow.api.exception.ValidationException;
import com.bankflow.api.repository.AccountRepository;
import com.bankflow.api.repository.LedgerEntryRepository;
import com.bankflow.api.repository.TransferRepository;
import com.bankflow.api.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock TransferRepository transferRepository;
    @Mock LedgerEntryRepository ledgerEntryRepository;
    @Mock AuditService auditService;

    @Test
    void withdrawalRejectsClosedAccount() {
        AccountService service = new AccountService(userRepository, accountRepository, transferRepository, ledgerEntryRepository, auditService);
        UUID userId = UUID.randomUUID();
        User owner = new User("a@test.dev", "A", "hash", Set.of(Role.CUSTOMER));
        ReflectionTestUtils.setField(owner, "id", userId);
        Account account = new Account(owner, "BF9", AccountType.CHECKING);
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.withdraw(userId, account.getId(), new MoneyRequest(new BigDecimal("1.00"), "cash")))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Closed accounts");
    }

    @Test
    void depositRejectsMoreThanTwoDecimalPlaces() {
        AccountService service = new AccountService(userRepository, accountRepository, transferRepository, ledgerEntryRepository, auditService);
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.deposit(userId, UUID.randomUUID(), new MoneyRequest(new BigDecimal("1.001"), "bad scale")))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("two decimal");
    }
}
