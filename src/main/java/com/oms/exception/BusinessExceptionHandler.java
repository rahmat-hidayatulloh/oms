package com.oms.exception;

import com.oms.config.ErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Handles business logic exceptions (RFC 7807).
 * Covers: ResourceNotFound, BusinessException, BadRequest, routing errors.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.oms")
public class BusinessExceptionHandler extends BaseExceptionHandler {

    public BusinessExceptionHandler(ErrorProperties errorProperties, HttpServletRequest request) {
        super(errorProperties, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(ErrorCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage());
        return build(ErrorCode.BUSINESS_ERROR, ex.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ProblemDetail handleNoHandlerFound(NoHandlerFoundException ex) {
        log.warn("No handler found: {}", ex.getMessage());
        return build(ErrorCode.NOT_FOUND,
                "Endpoint not found: " + ex.getRequestURL());
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ProblemDetail handleMissingPathVariable(
            MissingPathVariableException ex) {
        log.warn("Missing path variable: {}", ex.getMessage());
        return build(ErrorCode.BAD_REQUEST, ex.getMessage());
    }
}
