package com.fast.cqrs.eventsourcing.test;

import com.fast.cqrs.event.DomainEvent;
import com.fast.cqrs.eventsourcing.Aggregate;
import com.fast.cqrs.eventsourcing.InMemoryEventStore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Test fixture for aggregate testing with Given-When-Then DSL.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * AggregateTestFixture<OrderAggregate> fixture = AggregateTestFixture.create(OrderAggregate::new);
 * 
 * fixture
 *         .given(new OrderCreatedEvent("order-1", "customer-1"))
 *         .when(order -> order.ship())
 *         .then()
 *         .expectEvent(OrderShippedEvent.class)
 *         .expectState(order -> assertEquals("SHIPPED", order.getStatus()));
 * }</pre>
 */
public class AggregateTestFixture<T extends Aggregate> {

    private final Supplier<T> aggregateFactory;
    private final List<DomainEvent> givenEvents = new ArrayList<>();
    private T aggregate;
    private List<DomainEvent> producedEvents;
    private Exception thrownException;

    private AggregateTestFixture(Supplier<T> factory) {
        this.aggregateFactory = factory;
    }

    /**
     * Creates a test fixture for an aggregate.
     */
    public static <T extends Aggregate> AggregateTestFixture<T> create(Supplier<T> factory) {
        return new AggregateTestFixture<>(factory);
    }

    /**
     * Sets up the aggregate with existing events.
     */
    public AggregateTestFixture<T> given(DomainEvent... events) {
        givenEvents.addAll(List.of(events));
        return this;
    }

    /**
     * Sets up an empty aggregate.
     */
    public AggregateTestFixture<T> givenNoPriorEvents() {
        givenEvents.clear();
        return this;
    }

    /**
     * Executes a command on the aggregate.
     */
    public AggregateTestFixture<T> when(Consumer<T> command) {
        aggregate = aggregateFactory.get();

        // Replay given events
        for (DomainEvent event : givenEvents) {
            aggregate.replayEvent(event);
        }

        // Execute command
        try {
            command.accept(aggregate);
            producedEvents = aggregate.getUncommittedEvents();
            thrownException = null;
        } catch (Exception e) {
            thrownException = e;
            producedEvents = List.of();
        }

        return this;
    }

    /**
     * Starts the assertion phase.
     */
    public AssertionBuilder then() {
        return new AssertionBuilder();
    }

    /**
     * Builder for assertions.
     */
    public class AssertionBuilder {

        /**
         * Asserts that a specific event type was produced.
         */
        public AssertionBuilder expectEvent(Class<? extends DomainEvent> eventType) {
            if (producedEvents.stream().noneMatch(e -> eventType.isInstance(e))) {
                throw new AssertionError(
                        "Expected event " + eventType.getSimpleName() +
                                " but got: " + producedEvents);
            }
            return this;
        }

        /**
         * Asserts the number of events produced.
         */
        public AssertionBuilder expectEventCount(int count) {
            if (producedEvents.size() != count) {
                throw new AssertionError(
                        "Expected " + count + " events but got " + producedEvents.size());
            }
            return this;
        }

        /**
         * Asserts no events were produced.
         */
        public AssertionBuilder expectNoEvents() {
            return expectEventCount(0);
        }

        /**
         * Asserts the aggregate state.
         */
        public AssertionBuilder expectState(Consumer<T> stateAssertion) {
            stateAssertion.accept(aggregate);
            return this;
        }

        /**
         * Asserts an exception was thrown.
         */
        public AssertionBuilder expectException(Class<? extends Exception> exceptionType) {
            if (thrownException == null) {
                throw new AssertionError(
                        "Expected exception " + exceptionType.getSimpleName() + " but none was thrown");
            }
            if (!exceptionType.isInstance(thrownException)) {
                throw new AssertionError(
                        "Expected exception " + exceptionType.getSimpleName() +
                                " but got " + thrownException.getClass().getSimpleName());
            }
            return this;
        }

        /**
         * Asserts no exception was thrown.
         */
        public AssertionBuilder expectNoException() {
            if (thrownException != null) {
                throw new AssertionError("Expected no exception but got: " + thrownException);
            }
            return this;
        }

        /**
         * Gets the produced events.
         */
        public List<DomainEvent> getEvents() {
            return producedEvents;
        }

        /**
         * Gets the aggregate instance.
         */
        public T getAggregate() {
            return aggregate;
        }
    }
}
