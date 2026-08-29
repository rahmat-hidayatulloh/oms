package com.oms.ratelimit;

import com.oms.config.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for rate limiting.
 * <p>
 * Requires Docker/Testcontainers. Skipped automatically if Docker is not available.
 */
@DisabledIf("isDockerNotAvailable")
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Register a test user (requires register endpoint to exist)
        String username = "ratelimit_" + System.currentTimeMillis();
        String registerBody = """
                {"username":"%s","email":"%s@test.com","password":"StrongP@ss123!"}
                """.formatted(username, username);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> registerRequest = new HttpEntity<>(registerBody, headers);

        restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, String.class);

        // Login to get tokens
        String loginBody = """
                {"username":"%s","password":"StrongP@ss123!"}
                """.formatted(username);

        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, headers);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, String.class);

        if (loginResponse.getStatusCode().is2xxSuccessful() && loginResponse.getBody() != null) {
            String respBody = loginResponse.getBody();
            int tokenStart = respBody.indexOf("\"accessToken\":\"");
            if (tokenStart > 0) {
                tokenStart += 15;
                int tokenEnd = respBody.indexOf("\"", tokenStart);
                if (tokenEnd > tokenStart) {
                    authToken = respBody.substring(tokenStart, tokenEnd);
                }
            }
        }
    }

    @Test
    void shouldIncludeRateLimitHeadersInResponse() {
        HttpHeaders headers = new HttpHeaders();
        if (authToken != null) {
            headers.setBearerAuth(authToken);
        }

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/products",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().get("X-RateLimit-Limit")).isNotEmpty();
        assertThat(response.getHeaders().get("X-RateLimit-Remaining")).isNotEmpty();
        assertThat(response.getHeaders().get("X-RateLimit-Reset")).isNotEmpty();
    }

    @Test
    void shouldReturn429WhenRateLimitExceeded() {
        HttpHeaders headers = new HttpHeaders();
        if (authToken != null) {
            headers.setBearerAuth(authToken);
        }

        HttpStatus lastStatus = HttpStatus.OK;
        for (int i = 0; i < 100; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/products",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            lastStatus = HttpStatus.valueOf(response.getStatusCode().value());
            if (lastStatus == HttpStatus.TOO_MANY_REQUESTS) {
                break;
            }
        }

        assertThat(lastStatus).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldHaveDifferentBucketsForDifferentEndpoints() {
        HttpHeaders headers = new HttpHeaders();
        if (authToken != null) {
            headers.setBearerAuth(authToken);
        }

        HttpStatus productStatus = HttpStatus.OK;
        for (int i = 0; i < 100; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/products",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            productStatus = HttpStatus.valueOf(response.getStatusCode().value());
            if (productStatus == HttpStatus.TOO_MANY_REQUESTS) {
                break;
            }
        }

        ResponseEntity<String> orderResponse = restTemplate.exchange(
                "/api/v1/orders?page=0&size=1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(productStatus).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(orderResponse.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void shouldApplyRateLimitToAnonymousUsers() {
        HttpHeaders headers = new HttpHeaders();

        HttpStatus lastStatus = HttpStatus.OK;
        for (int i = 0; i < 100; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/products",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            lastStatus = HttpStatus.valueOf(response.getStatusCode().value());
            if (lastStatus == HttpStatus.TOO_MANY_REQUESTS) {
                break;
            }
        }

        assertThat(lastStatus).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
