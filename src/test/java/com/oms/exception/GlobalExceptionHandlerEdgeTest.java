package com.oms.exception;

import com.oms.config.ErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests edge cases in the domain-specific exception handlers.
 * Validates BusinessException, BadRequestException, and ResourceNotFoundException
 * are handled correctly by their respective handler classes.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerEdgeTest {

    @Mock
    private HttpServletRequest request;

    private BusinessExceptionHandler businessHandler;
    private ValidationExceptionHandler validationHandler;

    private final ErrorProperties errorProperties = new ErrorProperties();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        businessHandler = new BusinessExceptionHandler(errorProperties, request);
        validationHandler = new ValidationExceptionHandler(errorProperties, request);
    }

    @Test
    void handleBusinessException_shouldReturnProblemDetail() {
        when(request.getRequestURI()).thenReturn("/api/test");
        BusinessException ex = new BusinessException("Business rule violated");

        ProblemDetail result = businessHandler.handleBusiness(ex);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(ErrorCode.BUSINESS_ERROR.getTitle());
        assertThat(result.getStatus()).isEqualTo(ErrorCode.BUSINESS_ERROR.getStatusValue());
        assertThat(result.getDetail()).isEqualTo("Business rule violated");
    }

    @Test
    void handleBadRequestException_shouldReturnProblemDetail() {
        when(request.getRequestURI()).thenReturn("/api/test");
        BadRequestException ex = new BadRequestException("Invalid input");

        ProblemDetail result = validationHandler.handleBadRequest(ex);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(ErrorCode.BAD_REQUEST.getTitle());
        assertThat(result.getStatus()).isEqualTo(ErrorCode.BAD_REQUEST.getStatusValue());
        assertThat(result.getDetail()).isEqualTo("Invalid input");
    }

    @Test
    void handleResourceNotFoundException_shouldReturnProblemDetail() {
        when(request.getRequestURI()).thenReturn("/api/test");
        ResourceNotFoundException ex =
                new ResourceNotFoundException("Product", "id", "123");

        ProblemDetail result = businessHandler.handleResourceNotFound(ex);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(ErrorCode.NOT_FOUND.getTitle());
        assertThat(result.getStatus()).isEqualTo(ErrorCode.NOT_FOUND.getStatusValue());
        assertThat(result.getDetail()).contains("Product");
    }

    @Test
    void buildProblemDetail_shouldUseDefaultErrorBaseUrl() {
        when(request.getRequestURI()).thenReturn("/api/v1/products/123");
        ResourceNotFoundException ex =
                new ResourceNotFoundException("Product", "id", "123");

        ProblemDetail result = businessHandler.handleResourceNotFound(ex);

        assertThat(result.getType().toString())
                .isEqualTo("https://api.oms.com/errors/404");
    }
}
