package com.fast.cqrs.eventsourcing.saga;

import com.fast.cqrs.event.DomainEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Base class for sagas (process managers).
 * <p>
 * Sagas coordinate long-running business processes across multiple aggregates.
 * They react to events and issue commands to progress the workflow.
 * <p>
 * Usage:
 * 
 * <pre>
 * {
 *     &#64;code
 *     public class OrderFulfillmentSaga extends Saga {
 * 
 *         private boolean paymentReceived;
 *         private boolean inventoryReserved;
 * 
 *         &#64;SagaEventHandler
 *         public void on(OrderCreatedEvent event) {
 *             // Start payment and inventory reservation
 *         }
 * 
 *         @SagaEventHandler
 *         public void on(PaymentReceivedEvent event) {
 *             paymentReceived = true;
 *             checkComplete();
 *         }
 * 
 *         private void checkComplete() {
 *             if (paymentReceived && inventoryReserved) {
 *                 complete();
 *             }
 *         }
 *     }
 * }
 * </pre>
 */
public abstract class Saga {

    private String sagaId;
    private SagaState state = SagaState.STARTED;
    private Instant startedAt;
    private Instant completedAt;
    private final List<String> processedEventIds = new ArrayList<>();
    private Duration timeout;
    private String correlationId;

    protected Saga() {
        this.sagaId = UUID.randomUUID().toString();
        this.startedAt = Instant.now();
    }

    protected Saga(String sagaId) {
        this.sagaId = sagaId;
        this.startedAt = Instant.now();
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public SagaState getState() {
        return state;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean hasProcessed(String eventId) {
        return processedEventIds.contains(eventId);
    }

    public void markProcessed(String eventId) {
        processedEventIds.add(eventId);
    }

    /**
     * Advances the saga to in-progress state.
     */
    protected void advance() {
        if (state == SagaState.STARTED) {
            state = SagaState.IN_PROGRESS;
        }
    }

    /**
     * Completes the saga successfully.
     */
    protected void complete() {
        state = SagaState.COMPLETED;
        completedAt = Instant.now();
        onComplete();
    }

    /**
     * Starts compensation for the saga.
     */
    protected void compensate() {
        state = SagaState.COMPENSATING;
        onCompensate();
    }

    /**
     * Marks compensation as complete.
     */
    protected void compensated() {
        state = SagaState.COMPENSATED;
        completedAt = Instant.now();
    }

    /**
     * Fails the saga permanently.
     */
    protected void fail() {
        state = SagaState.FAILED;
        completedAt = Instant.now();
        onFail();
    }

    /**
     * Called when saga completes.
     */
    protected void onComplete() {
    }

    /**
     * Called when compensation starts.
     */
    protected void onCompensate() {
    }

    /**
     * Called when saga fails.
     */
    protected void onFail() {
    }

    /**
     * Called when timeout occurs.
     */
    protected void onTimeout() {
        compensate();
    }

    /**
     * Checks if saga has timed out.
     */
    public boolean isTimedOut() {
        if (timeout == null)
            return false;
        return Duration.between(startedAt, Instant.now()).compareTo(timeout) > 0;
    }

    /**
     * Returns the saga type name.
     */
    public String getSagaType() {
        return getClass().getSimpleName();
    }
}
