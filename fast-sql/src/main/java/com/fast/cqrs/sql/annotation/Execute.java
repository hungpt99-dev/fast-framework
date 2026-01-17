package com.fast.cqrs.sql.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a data-modifying SQL statement on a repository method.
 * <p>
 * Use this for INSERT, UPDATE, DELETE, and other write operations.
 * The SQL is executed using Spring's {@code NamedParameterJdbcTemplate}.
 * Parameters are bound using the {@link Param} annotation.
 * <p>
 * Return type options:
 * <ul>
 *   <li>{@code void} → no return value</li>
 *   <li>{@code int} or {@code Integer} → number of rows affected</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * @Execute("INSERT INTO users(id, name, email) VALUES (:id, :name, :email)")
 * void insert(@Param("id") String id, @Param("name") String name, @Param("email") String email);
 *
 * @Execute(value = "UPDATE users SET status = :status WHERE id = :id", retry = 2)
 * int updateStatus(@Param("id") String id, @Param("status") String status);
 * }</pre>
 *
 * @see Select
 * @see Param
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Execute {

    /**
     * The SQL statement to execute.
     * <p>
     * Use named parameters (e.g., {@code :id}) for parameter binding.
     *
     * @return the SQL statement
     */
    String value();

    // ==================== Performance Options ====================

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
     * Number of retry attempts for transient failures.
     * <p>
     * 0 (default) = no retry.
     * Retries on deadlock, connection timeout, etc.
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
     * Enable metrics collection for this statement.
     * <p>
     * If true, collects execution count, time, and error rate.
     */
    boolean metrics() default false;

    /**
     * Metrics name override.
     * <p>
     * If empty, auto-generates from repository and method name.
     */
    String metricsName() default "";

    /**
     * Batch size for batch operations.
     * <p>
     * 0 (default) = execute as single statement.
     * Set to a positive value for batch insert/update.
     */
    int batchSize() default 0;
}
