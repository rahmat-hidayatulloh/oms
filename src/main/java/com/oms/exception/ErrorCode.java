package com.oms.exception;

import org.springframework.http.HttpStatus;

/**
 * Centralized error codes for RFC 7807 Problem Detail responses.
 * <p>
 * Each enum constant defines a unique error type, HTTP status, and human-readable title.
 * Used by domain-specific exception handlers (SecurityExceptionHandler,
 * ValidationExceptionHandler, BusinessExceptionHandler, InfrastructureExceptionHandler)
 * to produce consistent error responses.
 *
 * @see SecurityExceptionHandler
 * @see ValidationExceptionHandler
 * @see BusinessExceptionHandler
 * @see InfrastructureExceptionHandler
 */
public enum ErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad Request"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed Request Body"),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "Type Mismatch"),
    BUSINESS_ERROR(HttpStatus.BAD_REQUEST, "Business Error"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation Failed"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication Failed"),

    FORBIDDEN(HttpStatus.FORBIDDEN, "Access Denied"),

    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource Not Found"),

    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "Not Acceptable"),
    CONFLICT(HttpStatus.CONFLICT, "Resource Conflict"),
    CONCURRENCY_CONFLICT(HttpStatus.CONFLICT, "Concurrent Modification"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable"),
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "Gateway Timeout"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");

    private final HttpStatus httpStatus;
    private final String title;

    ErrorCode(HttpStatus httpStatus, String title) {
        this.httpStatus = httpStatus;
        this.title = title;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getTitle() {
        return title;
    }

    public int getStatusValue() {
        return httpStatus.value();
    }
}