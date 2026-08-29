package com.oms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for preventing Cross-Site Scripting (XSS) attacks.
 * <p>
 * Validates that a string field does not contain:
 * <ul>
 *   <li>HTML tags (e.g., {@code <script>}, {@code <iframe>})</li>
 *   <li>JavaScript event handlers (e.g., {@code onclick=}, {@code onerror=})</li>
 *   <li>JavaScript URIs (e.g., {@code javascript:})</li>
 * </ul>
 * <p>
 * Should be applied to user-facing string fields such as product names,
 * descriptions, usernames, and emails to ensure safe rendering in frontends.
 *
 * @see SafeHtmlValidator
 */
@Documented
@Constraint(validatedBy = SafeHtmlValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeHtml {

    String message() default "Input contains potentially unsafe HTML or script content";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
