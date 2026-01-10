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
 * @Select("SELECT * FROM users WHERE status = :status")
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
}
