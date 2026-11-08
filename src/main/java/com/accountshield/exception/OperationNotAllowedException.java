package com.accountshield.exception;

import org.springframework.http.HttpStatus;

public class OperationNotAllowedException extends ApiException {

    public OperationNotAllowedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
