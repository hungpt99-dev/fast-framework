package com.fast.cqrs.eventsourcing.domain;

import java.lang.annotation.*;

/**
 * Annotation to mark lifecycle callback methods in aggregates.
 * <p>
 * Supported lifecycle events:
 * <ul>
 * <li>{@code onCreate} - Called when aggregate is first created</li>
 * <li>{@code onLoad} - Called after aggregate is loaded from events</li>
 * <li>{@code onSave} - Called before aggregate is saved</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;AggregateRoot
 *     public class OrderAggregate extends Aggregate {
 * 
 *         @AggregateLifecycle(Lifecycle.ON_LOAD)
 *         private void afterLoad() {
 *             // Called after replaying events
 *         }
 *     }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AggregateLifecycle {

    Lifecycle value();

    enum Lifecycle {
        ON_CREATE,
        ON_LOAD,
        ON_SAVE
    }
}
