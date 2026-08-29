package com.oms.system;

import com.oms.config.annotation.ApiVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint for clients to obtain a CSRF token.
 * <p>
 * CSRF protection is enabled for state-changing requests (POST, PUT, PATCH,
 * DELETE)
 * as a defense-in-depth measure. Clients must include the CSRF token in the
 * {@code X-XSRF-TOKEN} header for state-changing requests.
 */
@RestController
@RequestMapping("/api/v1/auth")
@ApiVersion("v1")
@Tag(name = "Auth", description = "Auth: login, refresh, logout. Public endpoints for JWT token management.")
public class CsrfController {

    /**
     * Returns the CSRF token for the current session.
     * <p>
     * The token is also set as a cookie ({@code XSRF-TOKEN}) by Spring Security.
     * Clients should read the cookie and send it back in the {@code X-XSRF-TOKEN}
     * header for state-changing requests.
     *
     * @param csrfToken the CSRF token injected by Spring Security
     * @return a map containing the CSRF token value
     */
    @Operation(
        summary = "Get CSRF token",
        description = "Returns a CSRF token for the current session. Set as XSRF-TOKEN cookie. Public endpoint.",
        responses = {
            @ApiResponse(responseCode = "200", description = "CSRF token retrieved successfully",
                content = @Content(schema = @Schema(implementation = Map.class)))
        })
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of("csrfToken", csrfToken.getToken()));
    }
}