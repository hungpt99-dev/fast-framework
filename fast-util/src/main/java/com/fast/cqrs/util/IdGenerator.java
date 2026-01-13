package com.fast.cqrs.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID generation utilities.
 */
public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final char[] ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private IdGenerator() {}

    /**
     * Generates a random UUID string (no dashes).
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generates a UUID with dashes.
     */
    public static String uuidWithDashes() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a short ID (8 characters).
     */
    public static String shortId() {
        return uuid().substring(0, 8);
    }

    /**
     * Generates a random alphanumeric string.
     */
    public static String random(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC[RANDOM.nextInt(ALPHANUMERIC.length)]);
        }
        return sb.toString();
    }

    /**
     * Generates a random hex string.
     */
    public static String randomHex(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(HEX_CHARS[RANDOM.nextInt(16)]);
        }
        return sb.toString();
    }

    /**
     * Generates a prefixed ID.
     * Example: prefixedId("ORD") = "ORD-a1b2c3d4"
     */
    public static String prefixedId(String prefix) {
        return prefix + "-" + shortId();
    }

    /**
     * Generates a time-based ID (sortable).
     * Format: timestamp_sequence_random
     */
    public static String timeBasedId() {
        long timestamp = Instant.now().toEpochMilli();
        long seq = SEQUENCE.incrementAndGet() % 10000;
        return String.format("%d_%04d_%s", timestamp, seq, randomHex(4));
    }

    /**
     * Generates a numeric ID.
     */
    public static long numericId() {
        return Math.abs(RANDOM.nextLong());
    }
}
