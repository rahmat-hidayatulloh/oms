package com.oms.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link BusinessException}.
 * Verifies business exception constructors and behavior.
 */
class BusinessExceptionTest {

    @Test
    void constructor_shouldCreateExceptionWithMessage() {
        BusinessException exception = new BusinessException("Business rule violated");

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Business rule violated");
    }

    @Test
    void constructor_shouldCreateExceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");

        BusinessException exception = new BusinessException("Business rule violated", cause);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Business rule violated");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void exception_shouldBeRuntimeException() {
        BusinessException exception = new BusinessException("Test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void exception_shouldBeThrowable() {
        BusinessException exception = new BusinessException("Test");

        assertThat(exception).isInstanceOf(Throwable.class);
    }
}