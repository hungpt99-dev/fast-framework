package com.fast.cqrs.concurrent.annotation;

import java.lang.annotation.*;

/**
 * Enforces a hard timeout on method execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExecutionTimeout {
    
    /**
     * Timeout in milliseconds.
     */
    long ms();
}
