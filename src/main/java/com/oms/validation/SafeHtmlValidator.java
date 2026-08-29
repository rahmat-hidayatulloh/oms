package com.oms.validation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link SafeHtml} annotation.
 * <p>
 * Checks that a string value does not contain dangerous HTML/JavaScript patterns
 * that could lead to Cross-Site Scripting (XSS) attacks.
 * <p>
 * Validation is skipped for {@code null} values (use {@code @NotNull} for that).
 *
 * @see InputSanitizer
 */
public class SafeHtmlValidator implements ConstraintValidator<SafeHtml, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // @NotBlank handles null/empty
        }

        if (InputSanitizer.containsUnsafePatterns(value)) {
            return false;
        }

        if (value.matches(".*<[a-zA-Z][^>]*>.*")) {
            return false;
        }

        return true;
    }
}
