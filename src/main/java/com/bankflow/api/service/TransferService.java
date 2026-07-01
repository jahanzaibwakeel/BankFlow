package com.bankflow.api.service;

import com.bankflow.api.domain.Account;
import com.bankflow.api.domain.AccountStatus;
import com.bankflow.api.domain.AuditAction;
import com.bankflow.api.domain.IdempotencyKey;
import com.bankflow.api.domain.LedgerEntry;
import com.bankflow.api.domain.LedgerEntryType;
import com.bankflow.api.domain.Transfer;
import com.bankflow.api.domain.TransferStatus;
import com.bankflow.api.dto.TransferDtos.ReviewRequest;
import com.bankflow.api.dto.TransferDtos.TransferRequest;
import com.bankflow.api.dto.TransferDtos.TransferResponse;
import com.bankflow.api.exception.DuplicateRequestException;
import com.bankflow.api.exception.ForbiddenException;
import com.bankflow.api.exception.NotFoundException;
import com.bankflow.api.exception.ValidationException;
import com.bankflow.api.repository.AccountRepository;
import com.bankflow.api.repository.IdempotencyKeyRepository;
import com.bankflow.api.repository.LedgerEntryRepository;
import com.bankflow.api.repository.TransferRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private static final String OPERATION = "TRANSFER";

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final AuditService auditService;

    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository, LedgerEntryRepository ledgerEntryRepository, IdempotencyKeyRepository idempotencyKeyRepository, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TransferResponse transfer(UUID userId, TransferRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required for transfers");
        }
        String hash = hash(request);
        IdempotencyKey key = idempotencyKeyRepository.findForUpdate(userId.toString(), idempotencyKey, OPERATION).orElse(null);
        if (key != null) {
            if (!key.getRequestHash().equals(hash)) {
                throw new DuplicateRequestException("Idempotency key was already used with a different payload");
            }
            auditService.record(userId.toString(), AuditAction.IDEMPOTENT_REPLAY, "TRANSFER", key.getResourceId(), "Duplicate transfer request replayed");
            return toResponse(transferRepository.findById(UUID.fromString(key.getResourceId())).orElseThrow(() -> new NotFoundException("Transfer not found")));
        }

        try {
            key = idempotencyKeyRepository.saveAndFlush(new IdempotencyKey(userId.toString(), idempotencyKey, hash, OPERATION));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateRequestException("Duplicate in-flight idempotency key");
        }

        Transfer transfer = postTransfer(userId, request);
        key.complete(transfer.getId());
        return toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> list(UUID userId, Pageable pageable) {
        return transferRepository.findVisibleToUser(userId, pageable).map(this::toResponse);
    }

    @Transactional
    public TransferResponse review(UUID adminId, UUID transferId, ReviewRequest request) {
        Transfer transfer = transferRepository.findById(transferId).orElseThrow(() -> new NotFoundException("Transfer not found"));
        if (request.status() != TransferStatus.PENDING_REVIEW && request.status() != TransferStatus.REJECTED && request.status() != TransferStatus.COMPLETED) {
            throw new ValidationException("INVALID_REVIEW_STATUS", "Review status must be PENDING_REVIEW, REJECTED, or COMPLETED");
        }
        transfer.review(adminId.toString(), request.status(), request.note());
        auditService.record(adminId.toString(), AuditAction.TRANSFER_REVIEWED, "TRANSFER", transferId.toString(), "Transfer reviewed as " + request.status());
        return toResponse(transfer);
    }

    private Transfer postTransfer(UUID userId, TransferRequest request) {
        BigDecimal amount = normalize(request.amount());
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new ValidationException("INVALID_TRANSFER", "Source and destination accounts must differ");
        }
        List<Account> locked = accountRepository.findAllByIdForUpdate(List.of(request.sourceAccountId(), request.destinationAccountId()));
        if (locked.size() != 2) {
            throw new NotFoundException("Source or destination account not found");
        }
        Map<UUID, Account> accounts = locked.stream().collect(Collectors.toMap(Account::getId, Function.identity()));
        Account source = accounts.get(request.sourceAccountId());
        Account destination = accounts.get(request.destinationAccountId());
        if (!source.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Source account does not belong to current user");
        }
        ensureActive(source, "Source");
        ensureActive(destination, "Destination");
        if (source.getBalance().compareTo(amount) < 0) {
            throw new ValidationException("INSUFFICIENT_BALANCE", "Insufficient balance");
        }
        source.debit(amount);
        destination.credit(amount);
        Transfer transfer = transferRepository.save(new Transfer(source, destination, amount, request.description(), TransferStatus.COMPLETED));
        ledgerEntryRepository.save(new LedgerEntry(source, transfer, LedgerEntryType.DEBIT, amount, source.getBalance(), "TRANSFER_OUT"));
        ledgerEntryRepository.save(new LedgerEntry(destination, transfer, LedgerEntryType.CREDIT, amount, destination.getBalance(), "TRANSFER_IN"));
        auditService.record(userId.toString(), AuditAction.TRANSFER_COMPLETED, "TRANSFER", transfer.getId().toString(), "Transfer completed for " + amount);
        return transfer;
    }

    private void ensureActive(Account account, String label) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new ValidationException("ACCOUNT_CLOSED", label + " account is closed");
        }
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new ValidationException("ACCOUNT_FROZEN", label + " account is frozen");
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

    private String hash(TransferRequest request) {
        String payload = request.sourceAccountId() + "|" + request.destinationAccountId() + "|" + request.amount().setScale(2, RoundingMode.UNNECESSARY) + "|" + request.description();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public TransferResponse toResponse(Transfer transfer) {
        UUID sourceId = transfer.getSourceAccount() == null ? null : transfer.getSourceAccount().getId();
        UUID destinationId = transfer.getDestinationAccount() == null ? null : transfer.getDestinationAccount().getId();
        return new TransferResponse(transfer.getId(), sourceId, destinationId, transfer.getAmount(), transfer.getDescription(), transfer.getStatus(), transfer.getCreatedAt());
    }
}
