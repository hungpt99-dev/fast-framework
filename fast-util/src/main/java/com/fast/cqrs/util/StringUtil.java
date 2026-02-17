package com.fast.cqrs.util;

/**
 * String utility methods.
 */
public final class StringUtil {

    private StringUtil() {}

    /**
     * Checks if a string is null or empty.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Checks if a string is null, empty, or blank.
     */
    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * Checks if a string has text (not null, not empty, not blank).
     */
    public static boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

    /**
     * Returns default value if string is blank.
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /**
     * Truncates string to max length with ellipsis.
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        if (maxLength <= 3) {
            return str.substring(0, maxLength);
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Converts camelCase to snake_case.
     */
    public static String toSnakeCase(String str) {
        if (str == null) return null;
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Converts snake_case to camelCase.
     */
    public static String toCamelCase(String str) {
        if (str == null) return null;
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (char c : str.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                result.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return result.toString();
    }

    /**
     * Masks a string showing only first and last n characters.
     */
    public static String mask(String str, int visibleChars) {
        if (str == null || str.length() <= visibleChars * 2) {
            return "***";
        }
        return str.substring(0, visibleChars) + "***" + str.substring(str.length() - visibleChars);
    }
}
