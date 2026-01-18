package com.fast.cqrs.concurrent.annotation;

import com.fast.cqrs.concurrent.resilience.RejectHandler;
import com.fast.cqrs.concurrent.resilience.RejectPolicy;

import java.lang.annotation.*;

/**
 * Limits concurrency based on a SpEL key expression.
 * <p>
 * Example: @KeyedConcurrency(key = "#orderId", permits = 1)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KeyedConcurrency {

    /**
     * SpEL expression to derive the key.
     */
    String key();

    /**
     * Permits per key.
     */
    int permits() default 1;

    /**
     * Wait timeout in milliseconds.
     */
    long waitTimeoutMs() default 200;

    RejectPolicy rejectPolicy() default RejectPolicy.WAIT_TIMEOUT;

    String fallback() default "";
    
    Class<? extends RejectHandler> rejectHandler() default RejectHandler.class;
}
