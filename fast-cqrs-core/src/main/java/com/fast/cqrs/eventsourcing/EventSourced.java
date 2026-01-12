package com.fast.cqrs.eventsourcing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an aggregate as event-sourced.
 * <p>
 * Event-sourced aggregates store all changes as events instead of
 * updating current state directly.
 * <p>
 * Example:
 * <pre>{@code
 * @EventSourced
 * public class OrderAggregate extends Aggregate {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventSourced {
    
    /**
     * Snapshot interval - after how many events to create a snapshot.
     * Set to 0 to disable snapshots.
     */
    int snapshotEvery() default 100;
}
