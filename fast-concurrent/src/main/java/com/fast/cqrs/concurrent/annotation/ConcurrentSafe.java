package com.fast.cqrs.concurrent.annotation;

import java.lang.annotation.*;

/**
 * Marks a component as safe for concurrency by applying default limits.
 * <p>
 * This is a zero-config way to prevent overload.
 * Defaults are configured via properties (fast.concurrent.defaults.*).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConcurrentSafe {
}
