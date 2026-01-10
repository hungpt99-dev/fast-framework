package com.fast.cqrs.logging.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for safe object-to-string conversion for logging.
 * <p>
 * Masks or omits sensitive fields to prevent data leaks.
 */
public final class SafeLogUtil {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "passwd", "secret", "token", "apiKey", "apikey",
        "authorization", "auth", "credential", "credentials",
        "ssn", "socialSecurityNumber", "creditCard", "cardNumber"
    );

    private static final String MASKED = "***";

    private SafeLogUtil() {}

    /**
     * Converts arguments to a safe log string.
     *
     * @param args the method arguments
     * @return safe string representation
     */
    public static String safeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
            .map(SafeLogUtil::safeValue)
            .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Converts a single value to a safe log string.
     *
     * @param value the value
     * @return safe string representation
     */
    public static String safeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s && s.length() > 100) {
            return s.substring(0, 100) + "...[truncated]";
        }
        return String.valueOf(value);
    }

    /**
     * Checks if a field name is sensitive.
     *
     * @param fieldName the field name
     * @return true if sensitive
     */
    public static boolean isSensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lower = fieldName.toLowerCase();
        return SENSITIVE_FIELDS.stream().anyMatch(lower::contains);
    }

    /**
     * Masks a sensitive value.
     *
     * @return the masked string
     */
    public static String mask() {
        return MASKED;
    }
}
