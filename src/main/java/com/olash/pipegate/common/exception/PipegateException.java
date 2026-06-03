package com.olash.pipegate.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PipegateException extends RuntimeException {

    private final HttpStatus httpStatus;

    public PipegateException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
