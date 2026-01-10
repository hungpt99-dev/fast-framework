package com.fast.cqrs.util;

import java.util.Collection;
import java.util.Map;

/**
 * Assertion utility methods.
 * Throws IllegalArgumentException on failure.
 */
public final class Assert {

    private Assert() {}

    /**
     * Asserts that condition is true.
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Asserts that condition is false.
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Asserts that object is not null.
     */
    public static <T> T notNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    /**
     * Asserts that string has text.
     */
    public static String hasText(String str, String message) {
        if (!StringUtil.hasText(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }

    /**
     * Asserts that collection is not empty.
     */
    public static <T extends Collection<?>> T notEmpty(T collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * Asserts that map is not empty.
     */
    public static <T extends Map<?, ?>> T notEmpty(T map, String message) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * Asserts that array is not empty.
     */
    public static <T> T[] notEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    /**
     * Asserts that value is positive.
     */
    public static int positive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Asserts that value is positive.
     */
    public static long positive(long value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Asserts that value is non-negative.
     */
    public static int notNegative(int value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Asserts that value is within range (inclusive).
     */
    public static int inRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
