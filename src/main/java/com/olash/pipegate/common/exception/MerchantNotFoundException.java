package com.olash.pipegate.common.exception;

import org.springframework.http.HttpStatus;

public class MerchantNotFoundException extends PipegateException {
    public MerchantNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
