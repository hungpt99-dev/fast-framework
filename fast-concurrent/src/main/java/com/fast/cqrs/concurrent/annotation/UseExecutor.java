package com.fast.cqrs.concurrent.annotation;

import java.lang.annotation.*;

/**
 * Offloads validation/processing to a specific named executor.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseExecutor {
    
    /**
     * Name of the executor to use (consistent with ExecutorRegistry).
     */
    String value();
}
