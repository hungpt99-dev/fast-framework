package com.fast.cqrs.concurrent.executor;

import java.lang.annotation.*;

/**
 * Annotation to specify executor configuration.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * @ExecutorSpec(name = "db-io", type = ExecutorType.IO, maxThreads = 50)
 * public class DatabaseConfig {
 * }
 * }</pre>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExecutorSpec {

    /**
     * Executor name.
     */
    String name();

    /**
     * Executor type.
     */
    ExecutorType type() default ExecutorType.VIRTUAL;

    /**
     * Maximum threads (for non-virtual executors).
     */
    int maxThreads() default -1;

    /**
     * Queue size.
     */
    int queueSize() default 1000;

    /**
     * Keep-alive time in seconds.
     */
    int keepAlive() default 60;
}
