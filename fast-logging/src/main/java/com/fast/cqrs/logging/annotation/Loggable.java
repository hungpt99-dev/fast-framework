package com.fast.cqrs.logging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for explicit business event logging.
 * <p>
 * Use this for intentional, meaningful business actions such as:
 * <ul>
 *   <li>Creating an order</li>
 *   <li>Submitting a loan application</li>
 *   <li>Approving a transaction</li>
 * </ul>
 * <p>
 * <strong>Not for:</strong> debugging, tracing, or general logging.
 * <p>
 * Example:
 * <pre>{@code
 * @Loggable("Creating new order")
 * public void createOrder(CreateOrderCmd cmd) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {

    /**
     * The business event message to log.
     *
     * @return the log message
     */
    String value();

    /**
     * Whether to log method arguments.
     * Default: false (for security)
     *
     * @return true to log arguments
     */
    boolean logArgs() default false;
}
