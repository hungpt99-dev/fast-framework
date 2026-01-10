package com.fast.cqrs.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as a query operation (read-only).
 * <p>
 * Query operations are dispatched through the {@link com.fast.cqrs.bus.QueryBus}
 * and are expected to return data without modifying system state.
 * <p>
 * This annotation is mandatory for all read operations in CQRS controllers.
 * Methods without either {@code @Query} or {@code @Command} will cause
 * a fail-fast error at runtime.
 * <p>
 * Example:
 * <pre>{@code
 * @Query
 * @GetMapping("/{id}")
 * OrderDto getOrder(@PathVariable String id);
 * }</pre>
 *
 * @see Command
 * @see com.fast.cqrs.bus.QueryBus
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Query {

    /**
     * Optional description of the query operation.
     *
     * @return the description, if any
     */
    String value() default "";
}
