package com.oms.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link BadRequestException}.
 */
class BadRequestExceptionTest {

    @Test
    void constructor_shouldCreateWithMessage() {
        BadRequestException ex = new BadRequestException("Invalid request");

        assertThat(ex.getMessage()).isEqualTo("Invalid request");
    }

    @Test
    void constructor_shouldCreateWithMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");

        BadRequestException ex = new BadRequestException("Invalid request", cause);

        assertThat(ex.getMessage()).isEqualTo("Invalid request");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}