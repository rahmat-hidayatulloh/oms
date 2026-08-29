package com.oms.exception;

import com.oms.config.ErrorProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests general exception handlers across SecurityExceptionHandler
 * and InfrastructureExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerGeneralTest {

    @Mock
    private HttpServletRequest request;

    private SecurityExceptionHandler securityHandler;

    private final ErrorProperties errorProperties = new ErrorProperties();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        securityHandler = new SecurityExceptionHandler(errorProperties, request);
    }

    @Test
    void handleAuthenticationException_shouldReturn401() {
        when(request.getRequestURI()).thenReturn("/api/login");
        AuthenticationException ex =
                new AuthenticationException("Bad credentials") { };

        ProblemDetail result = securityHandler.handleAuthentication(ex);

        assertThat(result.getStatus())
                .isEqualTo(ErrorCode.UNAUTHORIZED.getStatusValue());
        assertThat(result.getTitle())
                .isEqualTo(ErrorCode.UNAUTHORIZED.getTitle());
    }

    @Test
    void handleAccessDeniedException_shouldReturn403() {
        when(request.getRequestURI()).thenReturn("/api/admin");
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ProblemDetail result = securityHandler.handleAccessDenied(ex);

        assertThat(result.getStatus())
                .isEqualTo(ErrorCode.FORBIDDEN.getStatusValue());
        assertThat(result.getTitle())
                .isEqualTo(ErrorCode.FORBIDDEN.getTitle());
    }
}

