package com.fast.cqrs.cqrs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables distributed tracing for a method.
 * <p>
 * Creates a new span for the method execution.
 * <p>
 * Example:
 * <pre>{@code
 * @Traced(spanName = "process-payment")
 * @Command
 * void processPayment(@RequestBody ProcessPaymentCmd cmd);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
    
    /**
     * Span name.
     * Default: method name
     */
    String spanName() default "";
    
    /**
     * Whether to log arguments.
     */
    boolean logArgs() default false;
    
    /**
     * Whether to log the result.
     */
    boolean logResult() default false;
}
