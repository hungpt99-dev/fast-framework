package com.fast.cqrs.cqrs.cache;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing duration strings.
 * <p>
 * Supports formats: "100ms", "5s", "30m", "1h", "7d"
 */
public final class DurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)(ms|s|m|h|d)");

    private DurationParser() {}

    /**
     * Parses a duration string.
     *
     * @param value the duration string (e.g., "5m", "30s", "1h")
     * @return the parsed Duration
     * @throws IllegalArgumentException if the format is invalid
     */
    public static Duration parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Duration string cannot be null or empty");
        }

        Matcher matcher = DURATION_PATTERN.matcher(value.trim().toLowerCase());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid duration format: '" + value + "'. Expected format: <number><unit> where unit is ms/s/m/h/d");
        }

        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        return switch (unit) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("Unknown duration unit: " + unit);
        };
    }

    /**
     * Parses a duration string, returning a default if the value is null or empty.
     *
     * @param value the duration string
     * @param defaultValue the default duration if value is empty
     * @return the parsed Duration or the default
     */
    public static Duration parseOrDefault(String value, Duration defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return parse(value);
    }
}
