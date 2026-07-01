package com.bankflow.api.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BankFlowException {
    public NotFoundException(String message) {
        super("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
