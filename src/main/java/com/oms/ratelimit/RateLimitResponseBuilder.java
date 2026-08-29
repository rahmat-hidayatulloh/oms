package com.oms.ratelimit;

import com.oms.constant.RateLimitConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Builds RFC 7807 ProblemDetail responses for 429 Too Many Requests.
 */
@Component
public class RateLimitResponseBuilder {

    private static final String PROBLEM_DETAIL_TEMPLATE =
            "{\"type\":\"" + RateLimitConstants.RATE_LIMIT_ERROR_URL + "\""
            + ",\"title\":\"Too Many Requests\""
            + ",\"status\":429"
            + ",\"detail\":\"" + RateLimitConstants.MSG_RATE_LIMIT_EXCEEDED + "\""
            + ",\"instance\":\"%s\""
            + ",\"timestamp\":\"%s\"}";


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
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String body = String.format(
                PROBLEM_DETAIL_TEMPLATE,
                request.getRequestURI(), LocalDateTime.now());

        response.getWriter().write(body);
        response.getWriter().flush();
    }
}
