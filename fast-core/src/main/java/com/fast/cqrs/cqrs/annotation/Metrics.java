package com.fast.cqrs.cqrs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables metrics collection for a method.
 * <p>
 * Collects:
 * <ul>
 *   <li>Execution count</li>
 *   <li>Execution time (min, max, avg)</li>
 *   <li>Error rate</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * @Metrics(name = "order.create")
 * @Command
 * void createOrder(@RequestBody CreateOrderCmd cmd);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Metrics {
    
    /**
     * Metric name.
     * Default: class.method
     */
    String name() default "";
    
    /**
     * Additional tags for the metric.
     * Format: "key=value"
     */
    String[] tags() default {};
}
