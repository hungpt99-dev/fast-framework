package com.fast.cqrs.cqrs.annotation;

import com.fast.cqrs.cqrs.CommandHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as a command operation (state-changing).
 * <p>
 * Command operations are dispatched through the {@link com.fast.cqrs.bus.CommandBus}.
 * <p>
 * Example with handler:
 * <pre>{@code
 * @Command(handler = CreateOrderHandler.class)
 * @PostMapping
 * void createOrder(@RequestBody CreateOrderCmd cmd);
 * }</pre>
 *
 * @see Query
 * @see com.fast.cqrs.bus.CommandBus
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Command {

    /**
     * Optional description.
     */
    String value() default "";

    /**
     * The handler class to route this command to.
     */
    Class<? extends CommandHandler<?>> handler() default DefaultHandler.class;

    /**
     * The command class to instantiate from method parameters.
     */
    Class<?> command() default Void.class;

    /**
     * Default placeholder handler.
     */
    interface DefaultHandler extends CommandHandler<Object> {}
}
