package com.bankflow.api.controller;

import com.bankflow.api.dto.AccountDtos.AccountResponse;
import com.bankflow.api.dto.AccountDtos.CreateAccountRequest;
import com.bankflow.api.dto.AccountDtos.MoneyRequest;
import com.bankflow.api.dto.ApiResponse;
import com.bankflow.api.dto.LedgerDtos.LedgerEntryResponse;
import com.bankflow.api.dto.PageResponse;
import com.bankflow.api.service.AccountService;
import com.bankflow.api.service.LedgerService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController extends BaseController {
    private final AccountService accountService;
    private final LedgerService ledgerService;

    public AccountController(AccountService accountService, LedgerService ledgerService) {
        this.accountService = accountService;
        this.ledgerService = ledgerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> create(Authentication authentication, @Valid @RequestBody CreateAccountRequest request) {
        return ok(accountService.create(userId(authentication), request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> list(Authentication authentication, Pageable pageable) {
        return page(accountService.list(userId(authentication), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> get(Authentication authentication, @PathVariable UUID id) {
        return ok(accountService.get(userId(authentication), id, isAdmin(authentication)));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<ApiResponse<AccountResponse>> deposit(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody MoneyRequest request) {
        return ok(accountService.deposit(userId(authentication), id, request));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<AccountResponse>> withdraw(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody MoneyRequest request) {
        return ok(accountService.withdraw(userId(authentication), id, request));
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<ApiResponse<PageResponse<LedgerEntryResponse>>> ledger(Authentication authentication, @PathVariable UUID id, Pageable pageable) {
        return page(ledgerService.ledger(userId(authentication), id, isAdmin(authentication), pageable));
    }
}
