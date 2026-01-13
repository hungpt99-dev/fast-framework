package com.fast.cqrs.cqrs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables batch processing for a command method.
 * <p>
 * Example:
 * <pre>{@code
 * @BatchCommand(maxSize = 100)
 * @Command
 * void createOrders(@RequestBody List<CreateOrderCmd> commands);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BatchCommand {
    
    /**
     * Maximum batch size.
     */
    int maxSize() default 100;
    
    /**
     * Whether to continue on error or fail the entire batch.
     */
    boolean continueOnError() default false;
}
