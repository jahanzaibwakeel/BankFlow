package com.bankflow.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.bankflow.api.domain.Account;
import com.bankflow.api.domain.AccountType;
import com.bankflow.api.domain.IdempotencyKey;
import com.bankflow.api.domain.Role;
import com.bankflow.api.domain.Transfer;
import com.bankflow.api.domain.TransferStatus;
import com.bankflow.api.domain.User;
import com.bankflow.api.dto.TransferDtos.TransferRequest;
import com.bankflow.api.exception.DuplicateRequestException;
import com.bankflow.api.exception.ForbiddenException;
import com.bankflow.api.exception.ValidationException;
import com.bankflow.api.repository.AccountRepository;
import com.bankflow.api.repository.IdempotencyKeyRepository;
import com.bankflow.api.repository.LedgerEntryRepository;
import com.bankflow.api.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
    @Mock AccountRepository accountRepository;
    @Mock TransferRepository transferRepository;
    @Mock LedgerEntryRepository ledgerEntryRepository;
    @Mock IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock AuditService auditService;

    TransferService transferService;
    UUID ownerId;
    Account source;
    Account destination;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountRepository, transferRepository, ledgerEntryRepository, idempotencyKeyRepository, auditService);
        ownerId = UUID.randomUUID();
        User owner = user(ownerId, "owner@test.dev");
        source = account(UUID.randomUUID(), owner, "BF1", new BigDecimal("100.00"));
        destination = account(UUID.randomUUID(), user(UUID.randomUUID(), "dest@test.dev"), "BF2", BigDecimal.ZERO);
    }

    @Test
    void transferDebitsSourceCreditsDestinationAndCreatesAudit() {
        TransferRequest request = new TransferRequest(source.getId(), destination.getId(), new BigDecimal("25.00"), "rent");
        when(idempotencyKeyRepository.findForUpdate(ownerId.toString(), "key-1", "TRANSFER")).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findAllByIdForUpdate(List.of(source.getId(), destination.getId()))).thenReturn(List.of(source, destination));
        when(transferRepository.save(any())).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            ReflectionTestUtils.setField(transfer, "id", UUID.randomUUID());
            return transfer;
        });

        var response = transferService.transfer(ownerId, request, "key-1");

        assertThat(response.amount()).isEqualByComparingTo("25.00");
        assertThat(source.getBalance()).isEqualByComparingTo("75.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("25.00");
        verify(ledgerEntryRepository, times(2)).save(any());
        verify(auditService).record(any(), any(), any(), any(), any());
    }

    @Test
    void transferRejectsInsufficientBalance() {
        TransferRequest request = new TransferRequest(source.getId(), destination.getId(), new BigDecimal("125.00"), "too much");
        when(idempotencyKeyRepository.findForUpdate(ownerId.toString(), "key-2", "TRANSFER")).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findAllByIdForUpdate(List.of(source.getId(), destination.getId()))).thenReturn(List.of(source, destination));

        assertThatThrownBy(() -> transferService.transfer(ownerId, request, "key-2"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Insufficient balance");
    }

    @Test
    void transferRejectsUnauthorizedSourceAccount() {
        TransferRequest request = new TransferRequest(destination.getId(), source.getId(), new BigDecimal("1.00"), "bad owner");
        when(idempotencyKeyRepository.findForUpdate(ownerId.toString(), "key-3", "TRANSFER")).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findAllByIdForUpdate(List.of(destination.getId(), source.getId()))).thenReturn(List.of(destination, source));

        assertThatThrownBy(() -> transferService.transfer(ownerId, request, "key-3"))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void idempotencyKeyWithDifferentPayloadIsRejected() {
        TransferRequest request = new TransferRequest(source.getId(), destination.getId(), new BigDecimal("10.00"), "new");
        IdempotencyKey existing = new IdempotencyKey(ownerId.toString(), "same-key", "different-hash", "TRANSFER");
        when(idempotencyKeyRepository.findForUpdate(ownerId.toString(), "same-key", "TRANSFER")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transferService.transfer(ownerId, request, "same-key"))
            .isInstanceOf(DuplicateRequestException.class);
    }

    @Test
    void idempotencyReplayReturnsOriginalTransfer() {
        Transfer transfer = new Transfer(source, destination, new BigDecimal("10.00"), "same", TransferStatus.COMPLETED);
        UUID transferId = UUID.randomUUID();
        ReflectionTestUtils.setField(transfer, "id", transferId);
        TransferRequest request = new TransferRequest(source.getId(), destination.getId(), new BigDecimal("10.00"), "same");
        ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
        when(idempotencyKeyRepository.findForUpdate(ownerId.toString(), "first", "TRANSFER")).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.saveAndFlush(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findAllByIdForUpdate(List.of(source.getId(), destination.getId()))).thenReturn(List.of(source, destination));
        when(transferRepository.save(any())).thenReturn(transfer);

        transferService.transfer(ownerId, request, "first");
        IdempotencyKey saved = captor.getValue();
        when(idempotencyKeyRepository.findForUpdate(ownerId.toString(), "first", "TRANSFER")).thenReturn(Optional.of(saved));
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        assertThat(transferService.transfer(ownerId, request, "first").id()).isEqualTo(transferId);
    }

    private User user(UUID id, String email) {
        User user = new User(email, "Test User", "hash", Set.of(Role.CUSTOMER));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Account account(UUID id, User owner, String number, BigDecimal balance) {
        Account account = new Account(owner, number, AccountType.CHECKING);
        ReflectionTestUtils.setField(account, "id", id);
        ReflectionTestUtils.setField(account, "balance", balance);
        return account;
    }
}
