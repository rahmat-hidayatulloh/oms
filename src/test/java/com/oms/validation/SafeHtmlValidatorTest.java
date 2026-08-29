package com.oms.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafeHtmlValidatorTest {

    private SafeHtmlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SafeHtmlValidator();
    }

    @Test
    void isValid_shouldReturnTrueForNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void isValid_shouldReturnTrueForBlank() {
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void isValid_shouldReturnTrueForNormalText() {
        assertThat(validator.isValid("Kaos Polos Merah", null)).isTrue();
        assertThat(validator.isValid("user@example.com", null)).isTrue();
        assertThat(validator.isValid("Product 123 - test!", null)).isTrue();
    }

    @Test
    void isValid_shouldReturnFalseForScriptTag() {
        assertThat(validator.isValid("<script>alert(1)</script>", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForEventHandlers() {
        assertThat(validator.isValid("<div onclick=alert(1)>", null)).isFalse();
        assertThat(validator.isValid("<img onerror=alert(1)>", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForJavaScriptUri() {
        assertThat(validator.isValid("javascript:alert(1)", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForIframe() {
        assertThat(validator.isValid("<iframe src=evil.com>", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnFalseForHtmlTag() {
        assertThat(validator.isValid("<b>bold</b>", null)).isFalse();
        assertThat(validator.isValid("<img src=x>", null)).isFalse();
    }

    @Test
    void isValid_shouldReturnTrueForAngleBracketsInNonTagContext() {
        // These are NOT HTML tags — the regex looks for <letter...>
        assertThat(validator.isValid("a < b > c", null)).isTrue();
        assertThat(validator.isValid("1 < 2 and 3 > 2", null)).isTrue();
    }
}
