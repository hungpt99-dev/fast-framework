package com.fast.cqrs.concurrent.spring;

import java.lang.annotation.*;

/**
 * Marks a method or class as parallel-aware for context propagation.
 * <p>
 * When applied, MDC and SecurityContext are automatically propagated
 * to any async/parallel operations within the method.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * @ParallelAware
 * public UserProfile loadProfile(Long userId) {
 *     // Context is automatically propagated
 *     return ParallelFlow.of()
 *             .task("user", () -> loadUser(userId))
 *             .task("orders", () -> loadOrders(userId))
 *             .execute();
 * }
 * }</pre>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParallelAware {

    /**
     * Whether to propagate MDC context.
     */
    boolean mdc() default true;

    /**
     * Whether to propagate SecurityContext.
     */
    boolean security() default true;

    /**
     * Custom ThreadLocal names to propagate.
     */
    String[] custom() default {};
}
