package com.oms.config;

import java.io.IOException;
import java.util.UUID;

import com.oms.constant.MDCConstants;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that adds structured logging context (MDC) for every incoming request.
 * <p>
 * Sets the following MDC keys for correlation and debugging:
 * <ul>
 *   <li>{@code traceId} — request identifier (from Micrometer Tracing if available, otherwise UUID)</li>
 *   <li>{@code userId} — authenticated user principal, if available</li>
 *   <li>{@code method} — HTTP method (GET, POST, PUT, DELETE, etc.)</li>
 *   <li>{@code path} — request URI path</li>
 * </ul>
 * <p>
 * When Micrometer Tracing (OpenTelemetry) is active, the {@code traceId} is already set
 * by the tracing infrastructure. This filter skips generating its own traceId in that case
 * to avoid overwriting the distributed tracing traceId.
 * <p>
 * MDC context is automatically cleaned up in the {@code finally} block
 * to prevent memory leaks in thread-pooled environments.
 * <p>
 * Ordered with {@code HIGHEST_PRECEDENCE} to run before security filters,
 * ensuring traceId is available for all downstream logging.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Use Micrometer Tracing traceId if already set (distributed tracing active),
            // otherwise generate a UUID-based traceId for local correlation.
            String traceId = MDC.get(MDCConstants.TRACE_ID);
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
                MDC.put(MDCConstants.TRACE_ID, traceId);
            }

            String method = request.getMethod();
            String path = getRequestPath(request);
            MDC.put(MDCConstants.METHOD, method);
            MDC.put(MDCConstants.PATH, path);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                MDC.put(MDCConstants.USER_ID, authentication.getName());
            }

            log.debug("Incoming request: {} {} [traceId={}]", method, path, traceId);

            filterChain.doFilter(request, response);

        } finally {
            // Always clear MDC to prevent memory leaks in thread pools
            MDC.clear();
        }
    }

    /**
     * Extracts the request URI path, excluding query parameters.
     */
    private static String getRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            return uri + "?" + queryString;
        }
        return uri;
    }
}