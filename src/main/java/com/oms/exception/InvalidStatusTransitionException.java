package com.oms.exception;

import java.io.Serial;

/**
 * Raised when an order status transition is not allowed (e.g. approving an
 * order that is not {@code PENDING}). Maps to HTTP 409 Conflict via
 * {@link ErrorCode#INVALID_STATUS_TRANSITION}.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}