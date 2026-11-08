package com.oms.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.exception.ErrorCode;
import com.oms.exception.ProblemDetailFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns <b>403 Forbidden</b> with an RFC 7807 {@code ProblemDetail} body when
 * an already authenticated request lacks the required role/authority.
 * <p>
 * Filter-chain denials never reach {@code SecurityExceptionHandler}, so the
 * envelope has to be written here to keep one error shape across the API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ProblemDetailFactory problemDetailFactory;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied for {} {}: {}",
                request.getMethod(), request.getRequestURI(),
                accessDeniedException != null ? accessDeniedException.getMessage() : "forbidden");

        response.setStatus(ErrorCode.FORBIDDEN.getStatusValue());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                problemDetailFactory.create(ErrorCode.FORBIDDEN,
                        "You don't have permission to access this resource",
                        request.getRequestURI()));
    }
}
