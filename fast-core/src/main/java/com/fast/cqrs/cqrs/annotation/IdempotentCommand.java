package com.fast.cqrs.cqrs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a command as idempotent, preventing duplicate execution.
 * <p>
 * Uses the idempotency key to detect and skip duplicate commands.
 * <p>
 * Example:
 * <pre>{@code
 * @IdempotentCommand(key = "#cmd.orderId")
 * @Command
 * void createOrder(@RequestBody CreateOrderCmd cmd);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentCommand {
    
    /**
     * SpEL expression to extract the idempotency key.
     */
    String key();
    
    /**
     * Time-to-live for idempotency records.
     * After this time, the same key can be used again.
     */
    String ttl() default "24h";
}
