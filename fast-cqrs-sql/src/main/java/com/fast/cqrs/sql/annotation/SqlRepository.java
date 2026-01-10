package com.fast.cqrs.sql.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a SQL Repository.
 * <p>
 * SQL Repositories provide explicit, SQL-first data access without ORM magic.
 * Methods must be annotated with either {@link Select} for queries or
 * {@link Execute} for data modifications.
 * <p>
 * Example:
 * <pre>{@code
 * @SqlRepository
 * public interface OrderRepository {
 *
 *     @Select("SELECT * FROM orders WHERE id = :id")
 *     Order findById(@Param("id") String id);
 *
 *     @Execute("INSERT INTO orders(id, total) VALUES (:id, :total)")
 *     void insert(@Param("id") String id, @Param("total") BigDecimal total);
 * }
 * }</pre>
 *
 * @see Select
 * @see Execute
 * @see Param
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqlRepository {

    /**
     * The suggested bean name for this repository.
     *
     * @return the bean name, or empty for default naming
     */
    String value() default "";
}
