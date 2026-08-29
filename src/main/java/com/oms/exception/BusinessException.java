package com.oms.exception;

import java.io.Serial;

/**
 * Custom business exception that maps to HTTP 400 Bad Request.
 * Used for domain/validation rule violations that are not input-binding errors.
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}