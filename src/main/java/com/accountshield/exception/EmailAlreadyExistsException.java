package com.accountshield.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {

    public EmailAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "Email is already registered");
    }
}
