package com.fast.cqrs.eventsourcing.domain;

import java.lang.annotation.*;

/**
 * Marks a class as an aggregate root.
 * <p>
 * Aggregate roots are the entry points to aggregate boundaries
 * and are responsible for maintaining consistency.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * @AggregateRoot
 * public class OrderAggregate extends Aggregate {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AggregateRoot {

    /**
     * The aggregate type name. Defaults to class name.
     */
    String value() default "";

    /**
     * Snapshot strategy for this aggregate.
     */
    SnapshotStrategy snapshotStrategy() default SnapshotStrategy.NONE;

    /**
     * Event count threshold for snapshotting (if using EVENT_COUNT strategy).
     */
    int snapshotEveryN() default 100;
}
