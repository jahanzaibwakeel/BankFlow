package com.bankflow.api.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BankFlowException {
    public ValidationException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
