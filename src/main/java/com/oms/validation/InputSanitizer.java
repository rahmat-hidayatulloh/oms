package com.oms.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * Utility class for sanitizing user input against Cross-Site Scripting (XSS) attacks.
 * <p>
 * Provides methods to:
 * <ul>
 *   <li>Strip HTML tags from strings</li>
 *   <li>Encode HTML special characters to their entity equivalents</li>
 * </ul>
 * <p>
 * Used as a defense-in-depth measure for all user-facing string fields
 * (product name, description, username, email, etc.).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InputSanitizer {

    /**
     * Maximum allowed length for a sanitized string to prevent resource exhaustion.
     */
    private static final int MAX_SANITIZED_LENGTH = 1000;

    /**
     * Strips all HTML/XML tags from the input string and encodes HTML special
     * characters to prevent XSS.
     * <p>
     * This is the recommended method for general-purpose input sanitization.
     *
     * @param input the raw user input
     * @return the sanitized string with HTML tags removed and entities encoded,
     *         or {@code null} if input is null
     */
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String stripped = stripHtmlTags(input);
        String encoded = encodeHtmlEntities(stripped);
        if (encoded.length() > MAX_SANITIZED_LENGTH) {
            return encoded.substring(0, MAX_SANITIZED_LENGTH);
        }
        return encoded;
    }

    /**
     * Strips all HTML/XML tags from the input string.
     * Removes anything between {@code <} and {@code >} (inclusive).
     *
     * @param input the raw string
     * @return the string with HTML tags removed, or {@code null} if input is null
     */
    public static String stripHtmlTags(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "");
    }

    /**
     * Encodes HTML special characters to their corresponding entities.
     * <p>
     * Encodes:
     * <ul>
     *   <li>{@code &} → {@code &amp;amp;}</li>
     *   <li>{@code <} → {@code &amp;lt;}</li>
     *   <li>{@code >} → {@code &amp;gt;}</li>
     *   <li>{@code "} → {@code &amp;quot;}</li>
     *   <li>{@code '} → {@code &amp;#x27;}</li>
     * </ul>
     *
     * @param input the raw string
     * @return the encoded string, or {@code null} if input is null
     */
    public static String encodeHtmlEntities(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * Validates that input does not contain potentially dangerous patterns
     * beyond basic HTML (e.g., event handlers, javascript: URIs).
     *
     * @param input the raw string
     * @return {@code true} if the input is safe, {@code false} if it contains
     *         suspicious patterns
     */
    public static boolean containsUnsafePatterns(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String lower = input.toLowerCase(Locale.ROOT);
        return lower.contains("javascript:")
                || lower.contains("onerror=")
                || lower.contains("onload=")
                || lower.contains("onclick=")
                || lower.contains("onmouseover=")
                || lower.contains("<script")
                || lower.contains("<iframe")
                || lower.contains("<object")
                || lower.contains("<embed")
                || lower.contains("<form")
                || lower.contains("data:text/html");
    }
}
