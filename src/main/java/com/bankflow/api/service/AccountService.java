package com.bankflow.api.service;

import com.bankflow.api.domain.Account;
import com.bankflow.api.domain.AccountStatus;
import com.bankflow.api.domain.AuditAction;
import com.bankflow.api.domain.LedgerEntry;
import com.bankflow.api.domain.LedgerEntryType;
import com.bankflow.api.domain.Transfer;
import com.bankflow.api.domain.TransferStatus;
import com.bankflow.api.domain.User;
import com.bankflow.api.dto.AccountDtos.AccountResponse;
import com.bankflow.api.dto.AccountDtos.CreateAccountRequest;
import com.bankflow.api.dto.AccountDtos.MoneyRequest;
import com.bankflow.api.exception.ForbiddenException;
import com.bankflow.api.exception.NotFoundException;
import com.bankflow.api.exception.ValidationException;
import com.bankflow.api.repository.AccountRepository;
import com.bankflow.api.repository.LedgerEntryRepository;
import com.bankflow.api.repository.TransferRepository;
import com.bankflow.api.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(UserRepository userRepository, AccountRepository accountRepository, TransferRepository transferRepository, LedgerEntryRepository ledgerEntryRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AccountResponse create(UUID userId, CreateAccountRequest request) {
        User owner = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Account account = accountRepository.save(new Account(owner, nextAccountNumber(), request.type()));
        auditService.record(userId.toString(), AuditAction.ACCOUNT_CREATED, "ACCOUNT", account.getId().toString(), "Account created");
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> list(UUID userId, Pageable pageable) {
        return accountRepository.findByOwnerId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID userId, UUID accountId, boolean admin) {
        Account account = admin ? accountRepository.findById(accountId).orElseThrow(() -> new NotFoundException("Account not found"))
            : accountRepository.findByIdAndOwnerId(accountId, userId).orElseThrow(() -> new ForbiddenException("Account does not belong to current user"));
        return toResponse(account);
    }

    @Transactional
    public AccountResponse deposit(UUID userId, UUID accountId, MoneyRequest request) {
        BigDecimal amount = normalize(request.amount());
        Account account = lockedOwnedAccount(userId, accountId);
        ensureCanMove(account);
        account.credit(amount);
        Transfer transfer = transferRepository.save(new Transfer(null, account, amount, request.description(), TransferStatus.COMPLETED));
        ledgerEntryRepository.save(new LedgerEntry(account, transfer, LedgerEntryType.CREDIT, amount, account.getBalance(), "DEPOSIT"));
        auditService.record(userId.toString(), AuditAction.DEPOSIT_COMPLETED, "ACCOUNT", accountId.toString(), "Deposit completed for " + amount);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse withdraw(UUID userId, UUID accountId, MoneyRequest request) {
        BigDecimal amount = normalize(request.amount());
        Account account = lockedOwnedAccount(userId, accountId);
        ensureCanMove(account);
        ensureSufficient(account, amount);
        account.debit(amount);
        Transfer transfer = transferRepository.save(new Transfer(account, null, amount, request.description(), TransferStatus.COMPLETED));
        ledgerEntryRepository.save(new LedgerEntry(account, transfer, LedgerEntryType.DEBIT, amount, account.getBalance(), "WITHDRAWAL"));
        auditService.record(userId.toString(), AuditAction.WITHDRAWAL_COMPLETED, "ACCOUNT", accountId.toString(), "Withdrawal completed for " + amount);
        return toResponse(account);
    }

    @Transactional
    public void updateStatus(UUID adminId, UUID accountId, AccountStatus status) {
        Account account = accountRepository.findByIdForUpdate(accountId).orElseThrow(() -> new NotFoundException("Account not found"));
        account.setStatus(status);
        auditService.record(adminId.toString(), AuditAction.ACCOUNT_STATUS_CHANGED, "ACCOUNT", accountId.toString(), "Status changed to " + status);
    }

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getAccountNumber(), account.getType(), account.getStatus(), account.getBalance(), account.getCreatedAt());
    }

    private Account lockedOwnedAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findByIdForUpdate(accountId).orElseThrow(() -> new NotFoundException("Account not found"));
        if (!account.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Account does not belong to current user");
        }
        return account;
    }

    private void ensureCanMove(Account account) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new ValidationException("ACCOUNT_CLOSED", "Closed accounts cannot move money");
        }
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new ValidationException("ACCOUNT_FROZEN", "Frozen accounts cannot move money");
        }
    }

    private void ensureSufficient(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new ValidationException("INSUFFICIENT_BALANCE", "Insufficient balance");
        }
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount.scale() > 2) {
            throw new ValidationException("INVALID_AMOUNT", "Amount cannot have more than two decimal places");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (normalized.signum() <= 0) {
            throw new ValidationException("INVALID_AMOUNT", "Amount must be greater than zero");
        }
        return normalized;
    }

    private String nextAccountNumber() {
        String candidate;
        do {
            candidate = "BF" + (1000000000L + Math.abs(secureRandom.nextLong() % 9000000000L));
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}
