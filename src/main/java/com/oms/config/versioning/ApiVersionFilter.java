package com.oms.config.versioning;

import java.io.IOException;

import com.oms.constant.OpenApiConstants;

import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that adds deprecation warning headers for deprecated API versions.
 * <p>
 * When a deprecated API version is accessed, the response includes:
 * <ul>
 * <li>{@code X-API-Deprecated: v1} — the deprecated version</li>
 * <li>{@code X-API-Deprecation-Replacement: v2} — the suggested
 * replacement</li>
 * <li>{@code Sunset: ...} — planned sunset date (RFC 8594)</li>
 * </ul>
 * <p>
 * Runs after the logging filter but before security filters.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ApiVersionFilter extends OncePerRequestFilter {

    private final VersioningResolver versioningResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            String version = versioningResolver.resolveVersion(request);
            String replacement = versioningResolver.getDeprecationReplacement(version);

            if (replacement != null) {
                response.setHeader(OpenApiConstants.HEADER_DEPRECATED, version);
                response.setHeader(OpenApiConstants.HEADER_REPLACEMENT, replacement);
                response.setHeader(OpenApiConstants.HEADER_SUNSET, OpenApiConstants.SUNSET_DATE);

                log.warn("Deprecated API version '{}' accessed at {}. Replace with '{}'",
                        version, uri, replacement);
            }
        }

        filterChain.doFilter(request, response);
    }
}