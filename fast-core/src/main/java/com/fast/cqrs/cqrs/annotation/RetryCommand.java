package com.fast.cqrs.cqrs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables automatic retry for a command on transient failures.
 * <p>
 * Example:
 * <pre>{@code
 * @RetryCommand(maxAttempts = 3, backoff = "100ms")
 * @Command
 * void processPayment(@RequestBody ProcessPaymentCmd cmd);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryCommand {
    
    /**
     * Maximum number of attempts (including the initial call).
     */
    int maxAttempts() default 3;
    
    /**
     * Backoff delay between retries.
     * Format: number + unit (ms/s)
     * Examples: "100ms", "1s"
     */
    String backoff() default "100ms";
    
    /**
     * Multiplier for exponential backoff.
     * 1.0 = fixed delay, 2.0 = double each retry
     */
    double multiplier() default 1.0;
    
    /**
     * Exception types to retry on.
     * Default: all exceptions.
     */
    Class<? extends Throwable>[] retryOn() default {};
}
