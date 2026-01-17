package com.fast.cqrs.cqrs.gateway;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Gateway for dispatching commands with fluent configuration options.
 * <p>
 * Provides a higher-level API compared to {@link com.fast.cqrs.cqrs.CommandBus}
 * with support for timeouts, retries, async execution, and callbacks.
 * <p>
 * Example:
 * <pre>{@code
 * // Simple sync send
 * gateway.send(new CreateOrderCmd(...));
 * 
 * // Fire and forget
 * gateway.sendAndForget(new NotificationCmd(...));
 * 
 * // Fluent API
 * gateway.with(new CreateOrderCmd(...))
 *     .timeout(Duration.ofSeconds(5))
 *     .retry(3)
 *     .onSuccess(result -> log.info("Done"))
 *     .onError(e -> alertService.notify(e))
 *     .send();
 * }</pre>
 *
 * @see QueryGateway
 */
public interface CommandGateway {

    /**
     * Send a command and wait for completion.
     *
     * @param command the command to send
     * @param <R> optional result type (use Void for no result)
     * @return the result (may be null for void commands)
     */
    <R> R send(Object command);

    /**
     * Send a command with timeout.
     *
     * @param command the command to send
     * @param timeout maximum wait time
     * @param <R> result type
     * @return the result
     * @throws java.util.concurrent.TimeoutException if timeout expires
     */
    <R> R send(Object command, Duration timeout);

    /**
     * Send a command asynchronously.
     *
     * @param command the command to send
     * @param <R> result type
     * @return future with result
     */
    <R> CompletableFuture<R> sendAsync(Object command);

    /**
     * Fire-and-forget: send command without waiting for result.
     * Errors are logged but not propagated.
     *
     * @param command the command to send
     */
    void sendAndForget(Object command);

    /**
     * Start building a command dispatch with fluent API.
     *
     * @param command the command to dispatch
     * @return builder for configuring dispatch options
     */
    CommandDispatch with(Object command);

    /**
     * Fluent builder for command dispatch configuration.
     */
    interface CommandDispatch {
        
        /**
         * Set execution timeout.
         */
        CommandDispatch timeout(Duration timeout);

        /**
         * Set retry count with default backoff.
         */
        CommandDispatch retry(int attempts);

        /**
         * Set retry count with custom backoff.
         */
        CommandDispatch retry(int attempts, Duration backoff);

        /**
         * Callback on successful execution.
         */
        CommandDispatch onSuccess(java.util.function.Consumer<Object> callback);

        /**
         * Callback on error.
         */
        CommandDispatch onError(java.util.function.Consumer<Throwable> callback);

        /**
         * Execute synchronously.
         */
        <R> R send();

        /**
         * Execute asynchronously.
         */
        <R> CompletableFuture<R> sendAsync();

        /**
         * Fire and forget.
         */
        void sendAndForget();
    }
}
