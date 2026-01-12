package com.fast.cqrs.eventsourcing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler within an aggregate.
 * <p>
 * The method should have a single parameter of the event type.
 * <p>
 * Example:
 * <pre>{@code
 * @ApplyEvent
 * private void on(OrderCreatedEvent event) {
 *     this.status = "CREATED";
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplyEvent {
}
