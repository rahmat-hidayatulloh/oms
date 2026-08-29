package com.oms.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ErrorProperties} binding ({@code app.error.base-url}).
 * Verifies:
 * <ul>
 *   <li>Default base URL matches the RFC 7807 documentation site</li>
 *   <li>Per-environment override via configuration property source</li>
 * </ul>
 */
class ErrorPropertiesTest {

    @Test
    void shouldDefaultToDocumentedErrorBaseUrl() {
        ErrorProperties props = new ErrorProperties();

        assertThat(props.getBaseUrl()).isEqualTo("https://api.oms.com/errors");
        assertThat(props.getBaseUrl()).isEqualTo(ErrorProperties.DEFAULT_BASE_URL);
    }

    @Test
    void shouldAllowCustomBaseUrl() {
        ErrorProperties props = new ErrorProperties();
        props.setBaseUrl("https://errors.internal.oms.com");

        assertThat(props.getBaseUrl()).isEqualTo("https://errors.internal.oms.com");
    }

    @Test
    void shouldBindBaseUrlFromConfigurationPropertySource() {
        new ApplicationContextRunner()
                .withUserConfiguration(ErrorPropertiesTestConfig.class)
                .withPropertyValues("app.error.base-url=https://staging-api.oms.com/errors")
                .run(context -> {
                    assertThat(context).hasSingleBean(ErrorProperties.class);
                    assertThat(context.getBean(ErrorProperties.class).getBaseUrl())
                            .isEqualTo("https://staging-api.oms.com/errors");
                });
    }

    @EnableConfigurationProperties(ErrorProperties.class)
    static class ErrorPropertiesTestConfig {
    }
}