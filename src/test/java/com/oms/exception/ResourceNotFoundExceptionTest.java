package com.oms.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ResourceNotFoundException}.
 */
class ResourceNotFoundExceptionTest {

    @Test
    void constructor_shouldCreateWithMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");

        assertThat(ex.getMessage()).isEqualTo("Resource not found");
    }

    @Test
    void constructor_shouldCreateWithResourceNameAndField() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Product", "id", "123");

        assertThat(ex.getMessage()).contains("Product");
        assertThat(ex.getMessage()).contains("123");
    }
}