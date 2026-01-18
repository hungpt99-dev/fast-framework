package com.fast.cqrs.cqrs.annotation;

import com.fast.cqrs.cqrs.CommandHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as a command operation (state-changing).
 * <p>
 * Command operations are dispatched through direct handler invocation for
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
 * @Command(handler = CreateOrderHandler.class)
 * @PostMapping
 * void createOrder(@RequestBody CreateOrderCmd cmd);
 * }</pre>
 * 
 * <h3>With Performance Options</h3>
 * <pre>{@code
 * @Command(handler = CreateOrderHandler.class, retry = 3, metrics = "orders.create")
 * @PostMapping
 * void createOrder(@Valid @RequestBody CreateOrderCmd cmd);
 * }</pre>
 *
 * @see Query
 * @see CommandHandler
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Command {

    /**
     * Optional description.
     */
    String value() default "";

    /**
     * The handler class to route this command to.
     * <p>
     * <b>REQUIRED for GraalVM compatibility.</b> The annotation processor
     * will report a compile error if this is not specified.
     * <p>
     * The handler is injected directly into the generated controller
     * and invoked without any runtime reflection or bus dispatch.
     * <p>
     * Example:
     * <pre>{@code
     * @Command(handler = CreateOrderHandler.class)
     * }</pre>
     */
    Class<? extends CommandHandler<?>> handler() default DefaultHandler.class;

    /**
     * The command class to instantiate from method parameters.
     */
    Class<?> command() default Void.class;

    // ==================== Performance Options ====================

    /**
     * Metrics name for this command.
     * <p>
     * If empty, no metrics are collected.
     * If set, collects execution count, time, and error rate.
     */
    String metrics() default "";

    /**
     * Number of retry attempts for transient failures.
     * <p>
     * 0 (default) = no retry.
     * Retries on exceptions that are marked as retryable.
     */
    int retry() default 0;

    /**
     * Backoff delay between retry attempts.
     * <p>
     * Format: number + unit (ms/s)
     * Examples: "100ms", "1s"
     * <p>
     * Default: "100ms"
     */
    String retryBackoff() default "100ms";

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
     * Idempotency key expression using SpEL.
     * <p>
     * If set, ensures the command is executed only once for the same key.
     * Duplicate requests return the cached result.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "#cmd.requestId"} - Use requestId from command</li>
     *   <li>{@code "#headers['X-Request-Id']"} - Use request header</li>
     * </ul>
     * <p>
     * Empty string (default) = not idempotent.
     */
    String idempotencyKey() default "";

    /**
     * Execute command asynchronously.
     * <p>
     * If true, the method returns immediately and command executes in background.
     * Return type should be {@code void} or {@code CompletableFuture}.
     */
    boolean async() default false;

    /**
     * Default placeholder handler.
     * <p>
     * <b>Note:</b> Using this default will cause a compile error.
     * You must specify an explicit handler class.
     */
    interface DefaultHandler extends CommandHandler<Object> {}
}
