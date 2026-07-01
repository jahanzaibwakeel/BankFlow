package com.bankflow.api.controller;

import com.bankflow.api.dto.ApiResponse;
import com.bankflow.api.dto.PageResponse;
import com.bankflow.api.dto.TransferDtos.TransferRequest;
import com.bankflow.api.dto.TransferDtos.TransferResponse;
import com.bankflow.api.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController extends BaseController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
        Authentication authentication,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody TransferRequest request
    ) {
        return ok(transferService.transfer(userId(authentication), request, idempotencyKey));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransferResponse>>> list(Authentication authentication, Pageable pageable) {
        return page(transferService.list(userId(authentication), pageable));
    }
}
