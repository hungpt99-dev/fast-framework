package com.fast.cqrs.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as a command operation (state-changing).
 * <p>
 * Command operations are dispatched through the {@link com.fast.cqrs.bus.CommandBus}
 * and are expected to modify system state. By CQRS convention, commands
 * typically do not return values (void methods).
 * <p>
 * This annotation is mandatory for all write operations in CQRS controllers.
 * Methods without either {@code @Query} or {@code @Command} will cause
 * a fail-fast error at runtime.
 * <p>
 * Example:
 * <pre>{@code
 * @Command
 * @PostMapping
 * void createOrder(@RequestBody CreateOrderCmd cmd);
 * }</pre>
 *
 * @see Query
 * @see com.fast.cqrs.bus.CommandBus
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Command {

    /**
     * Optional description of the command operation.
     *
     * @return the description, if any
     */
    String value() default "";
}
