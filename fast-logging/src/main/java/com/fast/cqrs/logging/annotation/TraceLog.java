package com.fast.cqrs.logging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables execution timing and tracing for a method.
 * <p>
 * This annotation is intended for:
 * <ul>
 *   <li>Controllers</li>
 *   <li>Application services</li>
 *   <li>Integration boundaries</li>
 * </ul>
 * <p>
 * The trace aspect logs:
 * <ul>
 *   <li>Method entry (DEBUG)</li>
 *   <li>Execution time (DEBUG)</li>
 *   <li>Slow execution warning (WARN) if exceeds {@code slowMs}</li>
 * </ul>
 * <p>
 * <strong>Note:</strong> This aspect never logs exceptions.
 * Exceptions are handled by the global exception handler.
 * <p>
 * Example:
 * <pre>{@code
 * @TraceLog(slowMs = 100)
 * public OrderDto getOrder(String id) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TraceLog {

    /**
     * Threshold in milliseconds for slow execution warnings.
     * Default: 500ms
     *
     * @return the slow threshold in milliseconds
     */
    long slowMs() default 500;

    /**
     * Whether to log method arguments.
     * Default: false (for security)
     *
     * @return true to log arguments
     */
    boolean logArgs() default false;

    /**
     * Whether to log method result.
     * Default: false (for security)
     *
     * @return true to log result
     */
    boolean logResult() default false;
}
