package com.olash.pipegate.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends PipegateException {
    public InvalidOtpException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
