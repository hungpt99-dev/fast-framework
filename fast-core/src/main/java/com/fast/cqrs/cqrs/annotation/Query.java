package com.fast.cqrs.cqrs.annotation;

import com.fast.cqrs.cqrs.QueryHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as a query operation (read-only).
 * <p>
 * Query operations are dispatched through the {@link com.fast.cqrs.bus.QueryBus}.
 * <p>
 * Example with handler:
 * <pre>{@code
 * @Query(handler = GetOrderHandler.class)
 * @GetMapping("/{id}")
 * OrderDto getOrder(@PathVariable String id);
 * }</pre>
 * 
 * Example with query class:
 * <pre>{@code
 * @Query(query = GetOrderQuery.class)
 * @GetMapping("/{id}")
 * OrderDto getOrder(@PathVariable String id);
 * }</pre>
 *
 * @see Command
 * @see com.fast.cqrs.bus.QueryBus
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Query {

    /**
     * Optional description.
     */
    String value() default "";

    /**
     * The handler class to route this query to.
     * Framework creates query from method parameters and invokes handler.
     */
    Class<? extends QueryHandler<?, ?>> handler() default DefaultHandler.class;

    /**
     * The query class to instantiate from method parameters.
     * If specified, framework creates this query object and dispatches to QueryBus.
     */
    Class<?> query() default Void.class;

    /**
     * Default placeholder handler.
     */
    interface DefaultHandler extends QueryHandler<Object, Object> {}
}
