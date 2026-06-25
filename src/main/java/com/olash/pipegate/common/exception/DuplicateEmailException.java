package com.olash.pipegate.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends PipegateException {
    public DuplicateEmailException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
