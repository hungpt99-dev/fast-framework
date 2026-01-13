package com.fast.cqrs.concurrent.executor;

import java.lang.annotation.*;

/**
 * Marks a method to run asynchronously on a named executor.
 * <p>
 * Usage:
 * 
 * <pre>
 * {@code
 * &#64;RunAsync("db-io")
 * public void processData() {
 *     // Runs on db-io executor
 * }
 * 
 * @RunAsync  // Uses virtual threads
 * public CompletableFuture<User> loadUser(Long id) {
 *     return userRepository.findById(id);
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RunAsync {

    /**
     * Executor name. Defaults to virtual threads.
     */
    String value() default "virtual";

    /**
     * Timeout in milliseconds. 0 means no timeout.
     */
    long timeout() default 0;

    /**
     * Number of retry attempts on failure.
     */
    int retry() default 0;
}
