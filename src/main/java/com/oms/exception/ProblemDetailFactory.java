package com.oms.exception;

import com.oms.config.ErrorProperties;

import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * Builds RFC 7807 {@link ProblemDetail} instances outside of Spring MVC's
 * {@code @ExceptionHandler} pipeline.
 * <p>
 * Spring Security rejects requests inside the filter chain, before any
 * controller (and therefore any {@code @RestControllerAdvice}) is involved.
 * Components such as {@code RestAuthenticationEntryPoint} and
 * {@code RestAccessDeniedHandler} need the exact same error envelope as the
 * MVC handlers, so the construction logic lives here and both paths share it.
 */
@Component
public class ProblemDetailFactory {

    private final ErrorProperties errorProperties;

    public ProblemDetailFactory(ErrorProperties errorProperties) {
        this.errorProperties = errorProperties;
    }

    /**
     * Creates a ProblemDetail with the project-wide envelope:
     * {@code type}, {@code title}, {@code status}, {@code detail},
     * {@code instance}, and {@code timestamp}.
     *
     * @param errorCode  the error code (HTTP status + title)
     * @param detail     human-readable explanation
     * @param instanceUri request URI the error refers to
     * @return the populated ProblemDetail
     */
    public ProblemDetail create(ErrorCode errorCode, String detail, String instanceUri) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                errorCode.getHttpStatus(), detail);
        pd.setTitle(errorCode.getTitle());
        pd.setType(URI.create(errorProperties.getBaseUrl() + "/" + errorCode.getStatusValue()));
        if (instanceUri != null && !instanceUri.isBlank()) {
            pd.setInstance(URI.create(instanceUri));
        }
        pd.setProperty("timestamp", LocalDateTime.now());
        return pd;
    }
}
