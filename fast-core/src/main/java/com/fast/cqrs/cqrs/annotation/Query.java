package com.fast.cqrs.cqrs.annotation;

import com.fast.cqrs.cqrs.QueryHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as a query operation (read-only).
 * <p>
 * Query operations are dispatched through direct handler invocation for
 * zero-overhead, GraalVM-compatible execution.
 * 
 * <h3>GraalVM Native Image Requirement</h3>
 * <p>
 * <b>IMPORTANT:</b> The {@link #handler()} attribute is <b>required</b> for
 * GraalVM native-image compatibility. The annotation processor will fail
 * compilation if a handler is not specified.
 * 
 * <h3>Example</h3>
 * <pre>{@code
 * @Query(handler = GetOrderHandler.class)
 * @GetMapping("/{id}")
 * OrderDto getOrder(@PathVariable String id, @ModelAttribute GetOrderQuery query);
 * }</pre>
 * 
 * <h3>With Performance Options</h3>
 * <pre>{@code
 * @Query(handler = GetOrderHandler.class, cache = "5m", metrics = "orders.get")
 * @GetMapping("/{id}")
 * OrderDto getOrder(@PathVariable String id, @ModelAttribute GetOrderQuery query);
 * }</pre>
 *
 * @see Command
 * @see QueryHandler
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Query {

    /**
     * Optional description.
     */
    String value() default "";

    /**
     * The handler class to route this query to.
     * <p>
     * <b>REQUIRED for GraalVM compatibility.</b> The annotation processor
     * will report a compile error if this is not specified.
     * <p>
     * The handler is injected directly into the generated controller
     * and invoked without any runtime reflection or bus dispatch.
     * <p>
     * Example:
     * <pre>{@code
     * @Query(handler = GetOrderHandler.class)
     * }</pre>
     */
    Class<? extends QueryHandler<?, ?>> handler() default DefaultHandler.class;

    /**
     * The query class to instantiate from method parameters.
     * If specified, framework creates this query object and dispatches to QueryBus.
     */
    Class<?> query() default Void.class;

    // ==================== Performance Options ====================

    /**
     * Cache TTL for query results.
     * <p>
     * Format: number + unit (s/m/h/d)
     * Examples: "30s", "5m", "1h", "1d"
     * <p>
     * Empty string (default) = no caching.
     */
    String cache() default "";

    /**
     * Cache key expression using SpEL.
     * <p>
     * If empty, auto-generates key from method name and parameters.
     * Examples:
     * <ul>
     *   <li>{@code "#query.id"} - Use the id property of the query parameter</li>
     *   <li>{@code "#p0"} - Use the first parameter</li>
     *   <li>{@code "'static-key'"} - Use a static key</li>
     * </ul>
     */
    String cacheKey() default "";

    /**
     * Metrics name for this query.
     * <p>
     * If empty, no metrics are collected.
     * If set, collects execution count, time, and error rate.
     */
    String metrics() default "";

    /**
     * Execution timeout.
     * <p>
     * Format: number + unit (ms/s/m)
     * Examples: "500ms", "5s", "1m"
     * <p>
     * Empty string (default) = no timeout.
     */
    String timeout() default "";

    /**
     * Default placeholder handler.
     * <p>
     * <b>Note:</b> Using this default will cause a compile error.
     * You must specify an explicit handler class.
     */
    interface DefaultHandler extends QueryHandler<Object, Object> {}
}
