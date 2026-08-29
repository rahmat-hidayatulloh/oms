package com.oms.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for enforcing strong password requirements.
 * <p>
 * Validates that a password contains at least:
 * <ul>
 * <li>8 characters</li>
 * <li>one uppercase letter</li>
 * <li>one lowercase letter</li>
 * <li>one digit</li>
 * <li>one special character</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {

    String message() default "Password must be at least 8 characters long and contain uppercase, "
            + "lowercase, a number, and a special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}