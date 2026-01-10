package com.fast.cqrs.modal;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Utility for formatting field values based on format patterns.
 * <p>
 * Supports:
 * <ul>
 *   <li>Date/Time formatting (LocalDate, LocalDateTime, Date)</li>
 *   <li>Number formatting (DecimalFormat patterns)</li>
 * </ul>
 */
public final class FieldFormatter {

    private FieldFormatter() {
        // Utility class
    }

    /**
     * Formats a value according to the given format pattern.
     *
     * @param value  the value to format
     * @param format the format pattern
     * @return the formatted value, or original if not formattable
     */
    public static Object format(Object value, String format) {
        if (value == null || format == null || format.isEmpty()) {
            return value;
        }

        // Date/Time formatting
        if (value instanceof LocalDate date) {
            return DateTimeFormatter.ofPattern(format).format(date);
        }
        if (value instanceof LocalDateTime dateTime) {
            return DateTimeFormatter.ofPattern(format).format(dateTime);
        }
        if (value instanceof TemporalAccessor temporal) {
            return DateTimeFormatter.ofPattern(format).format(temporal);
        }
        if (value instanceof Date date) {
            return new SimpleDateFormat(format).format(date);
        }

        // Number formatting
        if (value instanceof Number number) {
            try {
                return new DecimalFormat(format).format(number);
            } catch (IllegalArgumentException e) {
                return value;
            }
        }

        return value;
    }
}
