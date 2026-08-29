package com.oms.exception;

import com.oms.config.ErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

/**
 * Handles infrastructure and unexpected exceptions (RFC 7807).
 * Covers: Redis, DB integrity, async timeout, optimistic lock, fallback.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.oms")
public class InfrastructureExceptionHandler extends BaseExceptionHandler {

    public InfrastructureExceptionHandler(ErrorProperties errorProperties, HttpServletRequest request) {
        super(errorProperties, request);
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ProblemDetail handleRedisConnectionFailure(
            RedisConnectionFailureException ex) {
        log.error("Redis connection failed: {}", ex.getMessage());
        return build(ErrorCode.SERVICE_UNAVAILABLE,
                "Cache service is currently unavailable");
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ProblemDetail handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        log.warn("Async request timeout: {}", ex.getMessage());
        return build(ErrorCode.SERVICE_UNAVAILABLE,
                "Request processing timed out");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return build(ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return build(ErrorCode.CONFLICT,
                "Resource was modified by another request. Please retry.");
    }
}
