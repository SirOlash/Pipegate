package com.olash.pipegate.common.exception;

import org.springframework.http.HttpStatus;

public class AccountNotVerifiedException extends PipegateException {

    public AccountNotVerifiedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
