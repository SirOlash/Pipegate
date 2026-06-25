package com.olash.pipegate.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends PipegateException{

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
