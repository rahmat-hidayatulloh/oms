package com.oms.ratelimit;

import com.oms.constant.RateLimitConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that applies rate limiting to controller methods annotated
 * with {@link RateLimit}. Delegates IP/CIDR resolution to {@link IpResolver}
 * and 429 response construction to {@link RateLimitResponseBuilder}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final IpResolver ipResolver;
    private final RateLimitResponseBuilder responseBuilder;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        if (!properties.isEnabled() || !(handler instanceof HandlerMethod hm)) return true;
        String endpoint = resolveEndpointName(hm);
        if (endpoint == null) return true;
        String clientKey = ipResolver.resolve(request);
        RateLimitResult result = rateLimitService.tryConsume(endpoint, clientKey);
        response.setHeader(RateLimitConstants.HEADER_RATE_LIMIT_LIMIT, String.valueOf(result.limit()));
        response.setHeader(RateLimitConstants.HEADER_RATE_LIMIT_REMAINING, String.valueOf(result.remainingTokens()));
        response.setHeader(RateLimitConstants.HEADER_RATE_LIMIT_RESET, String.valueOf(result.resetSeconds()));

        if (!result.allowed()) {
            log.warn("Rate limit exceeded for '{}' by '{}'", endpoint, clientKey);
            responseBuilder.sendRateLimitResponse(response, request);
            return false;
        }
        return true;
    }

    /** Resolves endpoint name from @RateLimit — method first, then class. */
    private String resolveEndpointName(HandlerMethod hm) {
        RateLimit m = hm.getMethodAnnotation(RateLimit.class);
        if (m != null && StringUtils.hasText(m.name())) return m.name();
        RateLimit c = hm.getBeanType().getAnnotation(RateLimit.class);
        if (c != null && StringUtils.hasText(c.name())) return c.name();
        return hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName();
    }
}
