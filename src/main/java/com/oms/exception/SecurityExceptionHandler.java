package com.oms.exception;

import com.oms.config.ErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;

/**
 * Handles authentication and authorization exceptions (RFC 7807).
 * Covers: AuthenticationException, AccessDeniedException, JWT errors.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.oms")
public class SecurityExceptionHandler extends BaseExceptionHandler {

    public SecurityExceptionHandler(ErrorProperties errorProperties, HttpServletRequest request) {
        super(errorProperties, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return build(ErrorCode.UNAUTHORIZED, "Bad credentials");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return build(ErrorCode.FORBIDDEN,
                "You don't have permission to access this resource");
    }

    @ExceptionHandler({
        ExpiredJwtException.class,
        MalformedJwtException.class,
        SecurityException.class})
    public ProblemDetail handleJwtException(RuntimeException ex) {
        log.warn("JWT error: {}", ex.getMessage());
        return build(ErrorCode.UNAUTHORIZED, "Invalid or expired token");
    }
}
