package com.oms.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import com.oms.constant.SecurityConstants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for generating and validating JWT tokens.
 * <p>
 * Provides access token (short-lived) and refresh token (long-lived) generation.
 * Claims include: username (subject), role, token_type, issuedAt, expiration.
 * Default access token expiration is 1 hour (3600000 ms).
 * Default refresh token expiration is 7 days (604800000 ms).
 */

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:3600000}") long expirationMs,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs) {
        byte[] keyBytes = secret != null ? secret.trim().getBytes(StandardCharsets.UTF_8) : null;
        if (keyBytes == null || keyBytes.length == 0) {
            throw new IllegalArgumentException(SecurityConstants.ERROR_SECRET_EMPTY);
        }
        if (keyBytes.length < SecurityConstants.MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(SecurityConstants.ERROR_SECRET_LENGTH);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Generates an access JWT token (short-lived) containing the username and role.
     *
     * @param username the authenticated user's username
     * @param role     the user's role (e.g. "ADMIN", "CUSTOMER")
     * @return a signed JWT access token string
     */
    public String generateAccessToken(String username, String role) {
        return generateToken(username, role, "access", expirationMs);
    }

    /**
     * Generates a refresh JWT token (long-lived, 7 days) containing the username and role.
     *
     * @param username the authenticated user's username
     * @param role     the user's role (e.g. "ADMIN", "CUSTOMER")
     * @return a signed JWT refresh token string
     */
    public String generateRefreshToken(String username, String role) {
        return generateToken(username, role, "refresh", refreshExpirationMs);
    }

    private String generateToken(String username, String role, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim(SecurityConstants.CLAIM_ROLE, role)
                .claim(SecurityConstants.CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates the JWT token signature and expiration.
     *
     * @param token the JWT string
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the JWT string
     * @return the username stored in the token
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts the role claim from a JWT token.
     *
     * @param token the JWT string
     * @return the role stored in the token (e.g. "ADMIN", "CUSTOMER")
     */
    public String extractRole(String token) {
        return getClaims(token).get(SecurityConstants.CLAIM_ROLE, String.class);
    }

    /**
     * Extracts the token type claim from a JWT token.
     *
     * @param token the JWT string
     * @return the token type ("access" or "refresh")
     */
    public String extractTokenType(String token) {
        return getClaims(token).get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}