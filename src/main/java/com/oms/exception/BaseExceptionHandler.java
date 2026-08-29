package com.oms.exception;

import com.oms.config.ErrorProperties;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * Base class for domain-specific exception handlers.
 * Provides shared RFC 7807 ProblemDetail construction.
 *
 * <p>The RFC 7807 {@code type} base URL is externalized via
 * {@link ErrorProperties} ({@code app.error.base-url}) instead of a
 * hardcoded constant, making error URIs environment-aware.
 */
public abstract class BaseExceptionHandler {

    private final ErrorProperties errorProperties;
    private final HttpServletRequest request;

    protected BaseExceptionHandler(ErrorProperties errorProperties, HttpServletRequest request) {
        this.errorProperties = errorProperties;
        this.request = request;
    }

    /**
     * Builds an RFC 7807 ProblemDetail response.
     */
    protected ProblemDetail build(ErrorCode errorCode, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                errorCode.getHttpStatus(), detail);
        pd.setTitle(errorCode.getTitle());
        pd.setType(URI.create(errorProperties.getBaseUrl() + "/" + errorCode.getStatusValue()));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", LocalDateTime.now());
        return pd;
    }
}
