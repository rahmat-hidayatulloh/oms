package com.oms.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator for {@link PasswordStrength} annotation.
 * <p>
 * Enforces password policy:
 * <ul>
 * <li>Minimum 8 characters</li>
 * <li>At least one uppercase letter (A-Z)</li>
 * <li>At least one lowercase letter (a-z)</li>
 * <li>At least one digit (0-9)</li>
 * <li>At least one special character (!@#$%^&*()-_=+[]{};:,./?<>)</li>
 * </ul>
 */
public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, String> {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_\\-+=\\[\\]{};:,.<>?]).{8,}$");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true; // @NotBlank handles null/empty
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}