package com.oms.auth.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PasswordStrengthValidator}.
 * Verifies password policy enforcement.
 */
class PasswordStrengthValidatorTest {

    private PasswordStrengthValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordStrengthValidator();
    }

    @Test
    void isValid_shouldReturnTrueForStrongPassword() {
        assertThat(validator.isValid("Password123!", null)).isTrue();
    }

    @Test
    void isValid_shouldReturnTrueForPasswordWithAllSpecialChars() {
        assertThat(validator.isValid("Str0ng!@#$%^&*()", null)).isTrue();
    }

    @Test
    void isValid_shouldReturnTrueForNull() {
        // Null is handled by @NotBlank, validator should not reject it
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void isValid_shouldReturnFalseForShortPassword() {
        assertThat(validator.isValid("Pass1!", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForNoUppercase() {
        assertThat(validator.isValid("password123!", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForNoLowercase() {
        assertThat(validator.isValid("PASSWORD123!", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForNoDigit() {
        assertThat(validator.isValid("Password!!!", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForNoSpecialCharacter() {
        assertThat(validator.isValid("Password123", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForEmptyString() {
        assertThat(validator.isValid("", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForWhitespaceOnly() {
        assertThat(validator.isValid("        ", null)).isFalse();
    }
}