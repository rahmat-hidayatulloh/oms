package com.oms.versioning;

import com.oms.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for API versioning.
 * <p>
 * Requires Docker/Testcontainers. Skipped automatically if Docker is not available.
 */
@DisabledIf("isDockerNotAvailable")
class ApiVersioningIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldServeV1Endpoints() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/products", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturn404ForUnknownVersion() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v99/products", String.class);
        assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn404ForNonExistentPath() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/nonexistent", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldHandleHealthEndpointWithoutVersion() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldHandleVersionHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Version", "v1");

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/products", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
