package com.fast.cqrs.concurrent.annotation;

import com.fast.cqrs.concurrent.resilience.RejectHandler;
import com.fast.cqrs.concurrent.resilience.RejectPolicy;

import java.lang.annotation.*;

/**
 * Defines concurrency limits for a class or method.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConcurrentLimit {
    
    /**
     * Number of concurrent executions allowed.
     */
    int permits() default -1; // -1 means use default from config

    /**
     * Policy to handle rejections.
     */
    RejectPolicy rejectPolicy() default RejectPolicy.WAIT_TIMEOUT;

    /**
     * Max time to wait for a permit (ms).
     */
    long waitTimeoutMs() default -1; // -1 means use default

    /**
     * Name of fallback method to call on rejection (if policy is FALLBACK).
     */
    String fallback() default "";

    /**
     * Custom reject handler class.
     */
    Class<? extends RejectHandler> rejectHandler() default RejectHandler.class; // default to none

    /**
     * Whether to treat virtual threads differently (e.g. higher limits).
     * Currently a hint for future implementation.
     */
    boolean virtualAware() default false;
}
