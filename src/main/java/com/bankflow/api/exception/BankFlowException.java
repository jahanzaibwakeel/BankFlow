package com.bankflow.api.exception;

import org.springframework.http.HttpStatus;

public class BankFlowException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BankFlowException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
