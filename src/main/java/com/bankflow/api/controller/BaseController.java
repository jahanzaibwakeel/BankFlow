package com.bankflow.api.controller;

import com.bankflow.api.domain.Role;
import com.bankflow.api.dto.ApiResponse;
import com.bankflow.api.dto.PageResponse;
import com.bankflow.api.security.BankFlowPrincipal;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

abstract class BaseController {
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.ok(data, MDC.get("requestId")));
    }

    protected <T> ResponseEntity<ApiResponse<PageResponse<T>>> page(Page<T> page) {
        return ok(PageResponse.from(page));
    }

    protected UUID userId(Authentication authentication) {
        return ((BankFlowPrincipal) authentication.getPrincipal()).id();
    }

    protected boolean isAdmin(Authentication authentication) {
        return ((BankFlowPrincipal) authentication.getPrincipal()).roles().contains(Role.ADMIN);
    }
}
