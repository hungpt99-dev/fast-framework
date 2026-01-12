package com.fast.cqrs.eventsourcing.saga;

import java.lang.annotation.*;

/**
 * Marks a method as a saga event handler.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SagaEventHandler {

    /**
     * Association property to find existing saga.
     */
    String associationProperty() default "";
}
