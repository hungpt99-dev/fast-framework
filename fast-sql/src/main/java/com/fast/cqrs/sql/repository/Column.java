package com.fast.cqrs.sql.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a field to a specific column name.
 * <p>
 * If not specified, the column name is derived from the field name
 * using snake_case conversion (e.g., customerId → customer_id).
 * <p>
 * Example:
 * <pre>{@code
 * public class Order {
 *     @Column("cust_id")
 *     private Long customerId;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    
    /**
     * The column name.
     */
    String value();
}
