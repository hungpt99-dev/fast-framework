package com.fast.cqrs.sql.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorting configuration for queries.
 */
public record Sort(List<Order> orders) {

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
     * Single sort order.
     */
    public record Order(Direction direction, String property) {}
}
