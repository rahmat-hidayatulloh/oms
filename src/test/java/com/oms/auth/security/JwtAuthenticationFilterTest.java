package com.oms.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oms.auth.service.RefreshTokenServiceInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenServiceInterface refreshTokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtTokenProvider, refreshTokenService);
    }

    @Test
    void shouldExtractTokenAndSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtTokenProvider.validateToken("valid.token.here")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("valid.token.here")).thenReturn("access");
        when(refreshTokenService.isAccessTokenBlacklisted("valid.token.here")).thenReturn(false);
        when(jwtTokenProvider.extractUsername("valid.token.here")).thenReturn("admin");
        when(jwtTokenProvider.extractRole("valid.token.here")).thenReturn("ADMIN");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("admin", auth.getPrincipal());
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_ADMIN", auth.getAuthorities().iterator().next().getAuthority());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipWhenNoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipWhenNotBearerToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic somebase64==");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipWhenTokenIsInvalid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token.here");
        when(jwtTokenProvider.validateToken("invalid.token.here")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).extractUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipWhenTokenValidationThrowsException() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token");
        when(jwtTokenProvider.validateToken("bad.token")).thenThrow(new RuntimeException("JWT error"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSetAuthenticationWithCustomerRole() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer customer.token");
        when(jwtTokenProvider.validateToken("customer.token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("customer.token")).thenReturn("access");
        when(refreshTokenService.isAccessTokenBlacklisted("customer.token")).thenReturn(false);
        when(jwtTokenProvider.extractUsername("customer.token")).thenReturn("john");
        when(jwtTokenProvider.extractRole("customer.token")).thenReturn("CUSTOMER");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("john", auth.getPrincipal());
        assertEquals("ROLE_CUSTOMER", auth.getAuthorities().iterator().next().getAuthority());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipWhenTokenIsBlacklisted() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer blacklisted.token");
        when(jwtTokenProvider.validateToken("blacklisted.token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("blacklisted.token")).thenReturn("access");
        when(refreshTokenService.isAccessTokenBlacklisted("blacklisted.token")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).extractUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipWhenRoleIsNull() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer norole.token");
        when(jwtTokenProvider.validateToken("norole.token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("norole.token")).thenReturn("access");
        when(refreshTokenService.isAccessTokenBlacklisted("norole.token")).thenReturn(false);
        when(jwtTokenProvider.extractUsername("norole.token")).thenReturn("user");
        when(jwtTokenProvider.extractRole("norole.token")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectRefreshTokenAsBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer refresh.token.here");
        when(jwtTokenProvider.validateToken("refresh.token.here")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("refresh.token.here")).thenReturn("refresh");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).extractUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectTokenWithNullTokenType() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer notype.token");
        when(jwtTokenProvider.validateToken("notype.token")).thenReturn(true);
        when(jwtTokenProvider.extractTokenType("notype.token")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).extractUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
