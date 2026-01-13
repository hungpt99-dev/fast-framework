package com.fast.cqrs.sql.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the primary key (ID) of the entity.
 * <p>
 * Example:
 * <pre>{@code
 * public class Order {
 *     @Id
 *     private Long id;
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
}
