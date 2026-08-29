package com.oms.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CsrfController}.
 */
class CsrfControllerTest {

    private final CsrfController csrfController = new CsrfController();

    @Test
    void getCsrfToken_shouldReturnToken() {
        CsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "test-csrf-token-value");

        ResponseEntity<Map<String, String>> response = csrfController.getCsrfToken(csrfToken);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("csrfToken")).isEqualTo("test-csrf-token-value");
    }
}