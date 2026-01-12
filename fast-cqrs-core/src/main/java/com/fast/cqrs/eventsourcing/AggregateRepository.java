package com.fast.cqrs.eventsourcing;

import com.fast.cqrs.event.DomainEvent;
import com.fast.cqrs.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Repository for loading and saving event-sourced aggregates.
 * <p>
 * Example:
 * <pre>{@code
 * @Autowired
 * private AggregateRepository<OrderAggregate> orderRepository;
 * 
 * public void createOrder(CreateOrderCmd cmd) {
 *     OrderAggregate order = new OrderAggregate();
 *     order.create(cmd.customerId(), cmd.total());
 *     orderRepository.save(order);
 * }
 * 
 * public void shipOrder(String orderId) {
 *     OrderAggregate order = orderRepository.load(orderId);
 *     order.ship();
 *     orderRepository.save(order);
 * }
 * }</pre>
 */
public class AggregateRepository<T extends Aggregate> {

    private static final Logger log = LoggerFactory.getLogger(AggregateRepository.class);

    private final EventStore eventStore;
    private final EventBus eventBus;
    private final Class<T> aggregateClass;

    public AggregateRepository(EventStore eventStore, EventBus eventBus, Class<T> aggregateClass) {
        this.eventStore = eventStore;
        this.eventBus = eventBus;
        this.aggregateClass = aggregateClass;
    }

    /**
     * Loads an aggregate by replaying its events.
     */
    public T load(String aggregateId) {
        List<DomainEvent> events = eventStore.load(aggregateId);
        
        if (events.isEmpty()) {
            throw new AggregateNotFoundException("Aggregate not found: " + aggregateId);
        }

        T aggregate = createInstance();
        aggregate.setId(aggregateId);
        
        for (DomainEvent event : events) {
            aggregate.replayEvent(event);
        }
        
        log.debug("Loaded aggregate {} with {} events, version {}", 
                  aggregateId, events.size(), aggregate.getVersion());
        
        return aggregate;
    }

    /**
     * Loads an aggregate or returns empty if not found.
     */
    public Optional<T> findById(String aggregateId) {
        try {
            return Optional.of(load(aggregateId));
        } catch (AggregateNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Saves an aggregate by appending its uncommitted events.
     */
    public void save(T aggregate) {
        List<DomainEvent> uncommittedEvents = aggregate.getUncommittedEvents();
        
        if (uncommittedEvents.isEmpty()) {
            return;
        }

        eventStore.append(aggregate.getId(), uncommittedEvents, aggregate.getVersion());
        
        // Publish events after persisting
        for (DomainEvent event : uncommittedEvents) {
            eventBus.publish(event);
        }
        
        aggregate.setVersion(aggregate.getVersion() + uncommittedEvents.size());
        aggregate.clearUncommittedEvents();
        
        log.debug("Saved aggregate {} with {} new events", 
                  aggregate.getId(), uncommittedEvents.size());
    }

    private T createInstance() {
        try {
            return aggregateClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create aggregate instance", e);
        }
    }

    /**
     * Exception thrown when aggregate is not found.
     */
    public static class AggregateNotFoundException extends RuntimeException {
        public AggregateNotFoundException(String message) {
            super(message);
        }
    }
}
