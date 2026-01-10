package com.fast.cqrs.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Date and time utility methods.
 */
public final class DateUtil {

    public static final String ISO_DATE = "yyyy-MM-dd";
    public static final String ISO_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATETIME_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATETIME);

    private DateUtil() {}

    /**
     * Gets current date.
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * Gets current date-time.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Gets current timestamp in milliseconds.
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Formats date as ISO string.
     */
    public static String formatDate(LocalDate date) {
        return date != null ? DATE_FORMATTER.format(date) : null;
    }

    /**
     * Formats date-time as ISO string.
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? DATETIME_FORMATTER.format(dateTime) : null;
    }

    /**
     * Parses ISO date string.
     */
    public static LocalDate parseDate(String str) {
        return StringUtil.hasText(str) ? LocalDate.parse(str, DATE_FORMATTER) : null;
    }

    /**
     * Parses ISO date-time string.
     */
    public static LocalDateTime parseDateTime(String str) {
        return StringUtil.hasText(str) ? LocalDateTime.parse(str, DATETIME_FORMATTER) : null;
    }

    /**
     * Converts epoch millis to LocalDateTime.
     */
    public static LocalDateTime fromEpochMilli(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
    }

    /**
     * Converts LocalDateTime to epoch millis.
     */
    public static long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Checks if date is in the past.
     */
    public static boolean isPast(LocalDate date) {
        return date != null && date.isBefore(today());
    }

    /**
     * Checks if date-time is in the past.
     */
    public static boolean isPast(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isBefore(now());
    }

    /**
     * Gets days between two dates.
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }
}
