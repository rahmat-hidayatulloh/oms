package com.oms.ratelimit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.constant.RateLimitConstants;
import com.oms.exception.ErrorCode;
import com.oms.exception.ProblemDetailFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Builds RFC 7807 ProblemDetail responses for 429 Too Many Requests.
 * <p>
 * The envelope (including the {@code type} URI) is produced by
 * {@link ProblemDetailFactory}, which derives {@code type} from
 * {@code app.error.base-url}. Rate limiting therefore honours the same
 * environment-aware configuration as every other error response — the error
 * base URI is never hardcoded (standard 4.1 — externalize configuration).
 */
@Component
public class RateLimitResponseBuilder {

    private final ProblemDetailFactory problemDetailFactory;
    private final ObjectMapper objectMapper;

    public RateLimitResponseBuilder(ProblemDetailFactory problemDetailFactory,
                                    ObjectMapper objectMapper) {
        this.problemDetailFactory = problemDetailFactory;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a 429 response with RFC 7807 ProblemDetail body.
     *
     * @param response the HTTP response
     * @param request  the HTTP request (for instance URI)
     * @throws IOException if writing the response body fails
     */
    public void sendRateLimitResponse(HttpServletResponse response,
                                       HttpServletRequest request) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ProblemDetail problem = problemDetailFactory.create(
                ErrorCode.TOO_MANY_REQUESTS,
                RateLimitConstants.MSG_RATE_LIMIT_EXCEEDED,
                request.getRequestURI());

        try {
            response.getWriter().write(objectMapper.writeValueAsString(problem));
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to serialize the 429 ProblemDetail body", e);
        }
        response.getWriter().flush();
    }
}
