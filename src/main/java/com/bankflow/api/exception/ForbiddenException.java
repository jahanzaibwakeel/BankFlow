package com.bankflow.api.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BankFlowException {
    public ForbiddenException(String message) {
        super("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }
}
