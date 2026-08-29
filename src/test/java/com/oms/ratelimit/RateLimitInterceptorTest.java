package com.oms.ratelimit;

import com.oms.auth.controller.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RateLimitService rateLimitService;

    private RateLimitInterceptor interceptor;
    private IpResolver ipResolver;
    private RateLimitResponseBuilder responseBuilder;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        ipResolver = new IpResolver(properties);
        responseBuilder = new RateLimitResponseBuilder();
        interceptor = new RateLimitInterceptor(rateLimitService, properties, ipResolver, responseBuilder);
    }

    @Test
    void shouldApplyLoginPolicyToLoginEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.tryConsume("auth-login", "ip:192.0.2.10"))
                .thenReturn(new RateLimitResult(true, 4, 5, 0));

        boolean allowed = interceptor.preHandle(request, response, handlerMethod("login"));

        assertThat(allowed).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        verify(rateLimitService).tryConsume("auth-login", "ip:192.0.2.10");
    }

    @Test
    void shouldApplySharedAuthPolicyToRefreshAndLogoutEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.setRemoteAddr("192.0.2.11");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.tryConsume(eq("auth"), eq("ip:192.0.2.11")))
                .thenReturn(new RateLimitResult(true, 9, 10, 0));

        assertThat(interceptor.preHandle(request, response, handlerMethod("refresh"))).isTrue();
        assertThat(interceptor.preHandle(request, response, handlerMethod("logout"))).isTrue();

        verify(rateLimitService, org.mockito.Mockito.times(2)).tryConsume("auth", "ip:192.0.2.11");
    }

    @Test
    void shouldReturnRfc7807ResponseWhenLimitIsExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.12");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.tryConsume("auth-login", "ip:192.0.2.12"))
                .thenReturn(new RateLimitResult(false, 0, 5, 60));

        boolean allowed = interceptor.preHandle(request, response, handlerMethod("login"));

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("\"status\":429", "\"title\":\"Too Many Requests\"");
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        AuthController controller = new AuthController(null, null, null, null);
        return switch (methodName) {
            case "login" -> new HandlerMethod(controller,
                    AuthController.class.getMethod("login", com.oms.auth.dto.LoginRequest.class));
            case "refresh" -> new HandlerMethod(controller,
                    AuthController.class.getMethod("refresh", com.oms.auth.dto.RefreshTokenRequest.class));
            case "logout" -> new HandlerMethod(controller,
                    AuthController.class.getMethod("logout", com.oms.auth.dto.RefreshTokenRequest.class));
            default -> throw new IllegalArgumentException("Unsupported auth method: " + methodName);
        };
    }

    @Test
    void shouldIgnoreXForwardedForFromUntrustedSource() throws Exception {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustedProxies(new String[]{"10.0.0.1"});
        IpResolver customIpResolver = new IpResolver(props);
        interceptor = new RateLimitInterceptor(rateLimitService, props, customIpResolver, responseBuilder);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.tryConsume("auth-login", "ip:192.0.2.1"))
                .thenReturn(new RateLimitResult(true, 4, 5, 0));

        interceptor.preHandle(request, response, handlerMethod("login"));

        // Should use remoteAddr, not X-Forwarded-For, because remoteAddr is not trusted
        verify(rateLimitService).tryConsume("auth-login", "ip:192.0.2.1");
    }

    @Test
    void shouldTrustXForwardedForFromTrustedProxy() throws Exception {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustedProxies(new String[]{"10.0.0.1"});
        IpResolver customIpResolver = new IpResolver(props);
        interceptor = new RateLimitInterceptor(rateLimitService, props, customIpResolver, responseBuilder);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.tryConsume("auth-login", "ip:1.2.3.4"))
                .thenReturn(new RateLimitResult(true, 4, 5, 0));

        interceptor.preHandle(request, response, handlerMethod("login"));

        // Should use X-Forwarded-For because remoteAddr is a trusted proxy
        verify(rateLimitService).tryConsume("auth-login", "ip:1.2.3.4");
    }

    @Test
    void shouldUseRemoteAddrWhenNoTrustedProxiesConfigured() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.tryConsume("auth-login", "ip:192.0.2.1"))
                .thenReturn(new RateLimitResult(true, 4, 5, 0));

        interceptor.preHandle(request, response, handlerMethod("login"));

        // No trusted proxies configured, so X-Forwarded-For should be ignored
        verify(rateLimitService).tryConsume("auth-login", "ip:192.0.2.1");
    }
}