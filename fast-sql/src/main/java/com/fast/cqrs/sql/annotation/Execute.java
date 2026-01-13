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
 * @Execute("UPDATE users SET status = :status WHERE id = :id")
 * int updateStatus(@Param("id") String id, @Param("status") String status);
 *
 * @Execute("DELETE FROM users WHERE id = :id")
 * void delete(@Param("id") String id);
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
}
