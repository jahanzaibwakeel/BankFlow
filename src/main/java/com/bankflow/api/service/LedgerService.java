package com.bankflow.api.service;

import com.bankflow.api.dto.LedgerDtos.LedgerEntryResponse;
import com.bankflow.api.exception.ForbiddenException;
import com.bankflow.api.repository.AccountRepository;
import com.bankflow.api.repository.LedgerEntryRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> ledger(UUID userId, UUID accountId, boolean admin, Pageable pageable) {
        if (!admin && accountRepository.findByIdAndOwnerId(accountId, userId).isEmpty()) {
            throw new ForbiddenException("Account does not belong to current user");
        }
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
            .map(entry -> new LedgerEntryResponse(
                entry.getId(),
                entry.getAccount().getId(),
                entry.getTransfer() == null ? null : entry.getTransfer().getId(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getReference(),
                entry.getCreatedAt()
            ));
    }
}
