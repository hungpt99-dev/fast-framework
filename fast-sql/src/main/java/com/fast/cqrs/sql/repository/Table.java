package com.fast.cqrs.sql.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the database table name for an entity.
 * <p>
 * If not specified, the table name is derived from the class name
 * using snake_case conversion (e.g., OrderItem → order_item).
 * <p>
 * Example:
 * <pre>{@code
 * @Table("orders")
 * public class Order {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    
    /**
     * The table name.
     */
    String value();
}
