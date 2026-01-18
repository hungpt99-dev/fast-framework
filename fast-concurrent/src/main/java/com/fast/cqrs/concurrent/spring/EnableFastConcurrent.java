package com.fast.cqrs.concurrent.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables Fast Framework Concurrency features.
 * <p>
 * Activates:
 * <ul>
 *   <li>@ConcurrentLimit</li>
 *   <li>@ConcurrentSafe</li>
 *   <li>@KeyedConcurrency</li>
 *   <li>@UseExecutor</li>
 *   <li>@ExecutionTimeout</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(FastConcurrentConfiguration.class)
public @interface EnableFastConcurrent {
}
