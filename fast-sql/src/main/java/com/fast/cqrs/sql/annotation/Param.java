package com.fast.cqrs.sql.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to a named SQL parameter.
 * <p>
 * All method parameters that should be bound to SQL must be annotated
 * with this annotation. Unannotated parameters are ignored.
 * <p>
 * Example:
 * <pre>{@code
 * @Select("SELECT * FROM users WHERE id = :id AND status = :status")
 * User findByIdAndStatus(@Param("id") String id, @Param("status") String status);
 * }</pre>
 *
 * @see Select
 * @see Execute
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /**
     * The name of the SQL parameter to bind to.
     * <p>
     * This must match the named parameter in the SQL (without the colon).
     *
     * @return the parameter name
     */
    String value();
}
