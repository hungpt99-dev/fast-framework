# Events & Event Sourcing

## Modules

| Module | Package | Use Case |
|--------|---------|----------|
| `fast-cqrs-event` | `com.fast.cqrs.event` | Lightweight event publishing |
| `fast-cqrs-eventsourcing` | `com.fast.cqrs.eventsourcing` | Full event sourcing |

---

## Event-Driven (Lightweight)

### Define Events

```java
import com.fast.cqrs.event.DomainEvent;

public class OrderCreatedEvent extends DomainEvent {
    private final String customerId;
    
    public OrderCreatedEvent(String aggregateId, String customerId) {
        super(aggregateId);
        this.customerId = customerId;
    }
}
```

### Publish & Handle

```java
// Publish
@Autowired EventBus eventBus;
eventBus.publish(new OrderCreatedEvent(orderId, customerId));

// Handle
@Component
public class OrderCreatedHandler implements DomainEventHandler<OrderCreatedEvent> {
    @Override
    public void handle(OrderCreatedEvent event) {
        // Send email, update read model
    }
}
```

---

## Event Sourcing (Full)

### Aggregate

```java
import com.fast.cqrs.eventsourcing.*;

@EventSourced
public class OrderAggregate extends Aggregate {
    private String status;

    public void create(String customerId) {
        apply(new OrderCreatedEvent(getId(), customerId));
    }

    @ApplyEvent
    private void on(OrderCreatedEvent event) {
        this.status = "CREATED";
    }
}
```

### Repository

```java
AggregateRepository<OrderAggregate> repo = new AggregateRepository<>(
    eventStore, eventBus, OrderAggregate.class
);

// Load and modify
OrderAggregate order = repo.load(orderId);
order.ship();
repo.save(order);
```

### Event Stores

| Store | Use Case |
|-------|----------|
| `InMemoryEventStore` | Testing |
| `JdbcEventStore` | SQL database |
| `RedisEventStore` | Distributed |

**JDBC Setup:**
```sql
CREATE TABLE event_store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data TEXT NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

```java
@Bean
public EventStore eventStore(JdbcTemplate jdbcTemplate) {
    return new JdbcEventStore(jdbcTemplate);
}
```

---

## Event Bus

```java
// Sync
EventBus eventBus = new SimpleEventBus();

// Async (recommended)
EventBus eventBus = new AsyncEventBus();
```
