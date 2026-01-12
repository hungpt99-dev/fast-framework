package com.fast.cqrs.eventsourcing.projection;

import java.lang.annotation.*;

/**
 * Marks a method as a projection event handler.
 * <p>
 * The method should accept a single event parameter.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * @ProjectionHandler
 * public void on(OrderCreatedEvent event) {
 *     jdbcTemplate.update("INSERT INTO order_summary ...",
 *             event.getOrderId(), event.getTotal());
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProjectionHandler {

    /**
     * Whether this handler is idempotent.
     */
    boolean idempotent() default false;

    /**
     * Whether to run in a transaction.
     */
    boolean transactional() default true;
}
