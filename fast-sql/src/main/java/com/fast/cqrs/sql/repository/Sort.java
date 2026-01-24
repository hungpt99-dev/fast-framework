package com.fast.cqrs.sql.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Sorting configuration for queries.
 * <p>
 * Property names are validated to prevent SQL injection attacks.
 * Only alphanumeric characters and underscores are allowed.
 */
public record Sort(List<Order> orders) {

    /**
     * Pattern for valid property names: alphanumeric, underscores, and dots (for nested properties).
     * Must start with a letter or underscore.
     */
    private static final Pattern VALID_PROPERTY_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

    /**
     * Maximum length for property names to prevent DoS attacks.
     */
    private static final int MAX_PROPERTY_LENGTH = 128;

    /**
     * Validates a property name to prevent SQL injection.
     *
     * @param property the property name to validate
     * @throws IllegalArgumentException if the property name is invalid
     */
    private static void validateProperty(String property) {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException("Sort property cannot be null or empty");
        }
        if (property.length() > MAX_PROPERTY_LENGTH) {
            throw new IllegalArgumentException("Sort property exceeds maximum length: " + MAX_PROPERTY_LENGTH);
        }
        if (!VALID_PROPERTY_PATTERN.matcher(property).matches()) {
            throw new IllegalArgumentException("Invalid sort property name: '" + property + 
                "'. Only alphanumeric characters, underscores, and dots are allowed.");
        }
    }

    /**
     * Creates an unsorted Sort.
     */
    public static Sort unsorted() {
        return new Sort(List.of());
    }

    /**
     * Creates a Sort with ascending order.
     */
    public static Sort by(String... properties) {
        List<Order> orders = new ArrayList<>();
        for (String property : properties) {
            orders.add(new Order(Direction.ASC, property));
        }
        return new Sort(orders);
    }

    /**
     * Creates a Sort with specified direction.
     */
    public static Sort by(Direction direction, String... properties) {
        List<Order> orders = new ArrayList<>();
        for (String property : properties) {
            orders.add(new Order(direction, property));
        }
        return new Sort(orders);
    }

    /**
     * Checks if sorting is defined.
     */
    public boolean isSorted() {
        return !orders.isEmpty();
    }

    /**
     * Converts to SQL ORDER BY clause.
     */
    public String toSql() {
        if (orders.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) sb.append(", ");
            Order order = orders.get(i);
            sb.append(order.property()).append(" ").append(order.direction().name());
        }
        return sb.toString();
    }

    /**
     * Sort direction.
     */
    public enum Direction {
        ASC, DESC
    }

    /**
     * Single sort order with validated property name.
     */
    public record Order(Direction direction, String property) {
        /**
         * Compact constructor validates property name to prevent SQL injection.
         */
        public Order {
            validateProperty(property);
        }
    }
}
