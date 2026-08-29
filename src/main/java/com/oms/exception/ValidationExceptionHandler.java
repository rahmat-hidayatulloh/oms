package com.oms.exception;

import com.oms.config.ErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles request validation exceptions (RFC 7807).
 * Covers: @Valid failures, bind errors, type mismatches, malformed requests.
 */
@RestControllerAdvice(basePackages = "com.oms")
public class ValidationExceptionHandler extends BaseExceptionHandler {

    public ValidationExceptionHandler(ErrorProperties errorProperties, HttpServletRequest request) {
        super(errorProperties, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        return withFieldErrors(build(ErrorCode.VALIDATION_FAILED, "Request validation failed"), ex.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    public ProblemDetail handleBind(BindException ex) {
        return withFieldErrors(build(ErrorCode.VALIDATION_FAILED, "Request binding failed"), ex.getBindingResult());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        return build(ErrorCode.VALIDATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex) {
        return build(ErrorCode.BAD_REQUEST, ex.getMessage());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformed(HttpMessageNotReadableException ex) {
        return build(ErrorCode.MALFORMED_REQUEST, "Malformed or unreadable request body");
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> requiredType = ex.getRequiredType();
        String type = requiredType != null ? requiredType.getSimpleName() : "unknown";
        String msg = String.format("Parameter '%s' has invalid value '%s'. Expected: %s",
                ex.getName(), ex.getValue(), type);
        return build(ErrorCode.TYPE_MISMATCH, msg);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage());
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        return build(ErrorCode.BAD_REQUEST, ex.getMessage());
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(ErrorCode.BAD_REQUEST, ex.getMessage());
    }
    private ProblemDetail withFieldErrors(ProblemDetail pd, BindingResult br) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : br.getFieldErrors()) errors.put(fe.getField(), fe.getDefaultMessage());
        pd.setProperty("fieldErrors", errors);
        return pd;
    }
}
