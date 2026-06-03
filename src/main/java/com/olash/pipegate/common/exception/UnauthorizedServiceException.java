package com.olash.pipegate.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedServiceException extends PipegateException {
  public UnauthorizedServiceException(String message) {
    super(message, HttpStatus.FORBIDDEN);
  }
}
