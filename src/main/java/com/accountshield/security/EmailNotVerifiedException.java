package com.accountshield.security;

import org.springframework.security.authentication.AccountStatusException;

public class EmailNotVerifiedException extends AccountStatusException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
