package com.bankflow.api.exception;

import org.springframework.http.HttpStatus;

public class DuplicateRequestException extends BankFlowException {
    public DuplicateRequestException(String message) {
        super("DUPLICATE_REQUEST", message, HttpStatus.CONFLICT);
    }
}
