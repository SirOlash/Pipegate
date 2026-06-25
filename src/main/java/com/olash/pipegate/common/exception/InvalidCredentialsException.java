package com.olash.pipegate.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends PipegateException {

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
