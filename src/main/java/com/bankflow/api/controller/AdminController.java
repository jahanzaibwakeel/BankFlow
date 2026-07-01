package com.bankflow.api.controller;

import com.bankflow.api.domain.AccountStatus;
import com.bankflow.api.dto.ApiResponse;
import com.bankflow.api.dto.AuditDtos.AuditLogResponse;
import com.bankflow.api.dto.PageResponse;
import com.bankflow.api.dto.ReconciliationDtos.ReconciliationReport;
import com.bankflow.api.dto.TransferDtos.ReviewRequest;
import com.bankflow.api.dto.TransferDtos.TransferResponse;
import com.bankflow.api.service.AccountService;
import com.bankflow.api.service.AuditService;
import com.bankflow.api.service.ReconciliationService;
import com.bankflow.api.service.TransferService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {
    private final AuditService auditService;
    private final TransferService transferService;
    private final AccountService accountService;
    private final ReconciliationService reconciliationService;

    public AdminController(AuditService auditService, TransferService transferService, AccountService accountService, ReconciliationService reconciliationService) {
        this.auditService = auditService;
        this.transferService = transferService;
        this.accountService = accountService;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> auditLogs(Pageable pageable) {
        return page(auditService.all(pageable).map(log -> new AuditLogResponse(
            log.getId(), log.getActorUserId(), log.getAction(), log.getResourceType(), log.getResourceId(), log.getDetails(), log.getCreatedAt()
        )));
    }

    @PatchMapping("/transfers/{id}/review")
    public ResponseEntity<ApiResponse<TransferResponse>> review(Authentication authentication, @PathVariable UUID id, @Valid @RequestBody ReviewRequest request) {
        return ok(transferService.review(userId(authentication), id, request));
    }

    @PatchMapping("/accounts/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(Authentication authentication, @PathVariable UUID id, @RequestParam AccountStatus status) {
        accountService.updateStatus(userId(authentication), id, status);
        return ok(null);
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<ApiResponse<ReconciliationReport>> reconciliation() {
        return ok(reconciliationService.reconcile());
    }
}
