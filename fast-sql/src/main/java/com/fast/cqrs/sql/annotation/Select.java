package com.fast.cqrs.sql.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a read-only SQL query on a repository method.
 * <p>
 * The SQL is executed using Spring's {@code NamedParameterJdbcTemplate}.
 * Parameters are bound using the {@link Param} annotation.
 * <p>
 * Return type determines execution mode:
 * <ul>
 *   <li>Single object → {@code queryForObject}</li>
 *   <li>Collection → {@code query}</li>
 *   <li>{@code Optional<T>} → single result wrapped in Optional</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * @Select("SELECT id, name, email FROM users WHERE id = :id")
 * User findById(@Param("id") String id);
 *
 * @Select(value = "SELECT * FROM users WHERE status = :status", cache = "5m")
 * List<User> findByStatus(@Param("status") String status);
 * }</pre>
 *
 * @see Execute
 * @see Param
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Select {

    /**
     * The SQL query to execute.
     * <p>
     * Use named parameters (e.g., {@code :id}) for parameter binding.
     *
     * @return the SQL query
     */
    String value();

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
     *   <li>{@code "#id"} - Use the id parameter</li>
     *   <li>{@code "#p0 + ':' + #p1"} - Combine first two parameters</li>
     * </ul>
     */
    String cacheKey() default "";

    /**
     * Query execution timeout.
     * <p>
     * Format: number + unit (ms/s/m)
     * Examples: "500ms", "5s", "1m"
     * <p>
     * Empty string (default) = no timeout.
     */
    String timeout() default "";

    /**
     * JDBC fetch size hint for large result sets.
     * <p>
     * 0 (default) = use driver default.
     * Set to a lower value (e.g., 100) for memory-efficient streaming.
     */
    int fetchSize() default 0;

    /**
     * Enable metrics collection for this query.
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
}
