package com.oms.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.exception.ErrorCode;
import com.oms.exception.ProblemDetailFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns <b>401 Unauthorized</b> with an RFC 7807 {@code ProblemDetail} body
 * when a request reaches a protected endpoint without valid credentials.
 * <p>
 * Without an explicit entry point Spring Security decides between 401 and 403
 * from the configured mechanisms; with only a stateless JWT filter installed it
 * falls back to {@code Http403ForbiddenEntryPoint}, which makes every anonymous
 * call answer 403. Clients then cannot distinguish "log in first" (401) from
 * "you are logged in but not allowed" (403).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ProblemDetailFactory problemDetailFactory;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Unauthenticated access to {} {}: {}",
                request.getMethod(), request.getRequestURI(),
                authException != null ? authException.getMessage() : "no credentials");

        response.setStatus(ErrorCode.UNAUTHORIZED.getStatusValue());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                problemDetailFactory.create(ErrorCode.UNAUTHORIZED,
                        "Authentication is required to access this resource",
                        request.getRequestURI()));
    }
}
