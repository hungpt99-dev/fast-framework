# Event Sourcing

## Overview

Event sourcing stores all changes as a sequence of events instead of just the current state. This enables:
- Complete audit trail
- Time travel (rebuild past states)
- Event replay for debugging
- CQRS pattern support

## Quick Start

### 1. Define Events

```java
public class OrderCreatedEvent extends DomainEvent {
    private final String customerId;
    private final BigDecimal total;
    
    public OrderCreatedEvent(String aggregateId, String customerId, BigDecimal total) {
        super(aggregateId);
        this.customerId = customerId;
        this.total = total;
    }
    // getters...
}

public class OrderShippedEvent extends DomainEvent {
    public OrderShippedEvent(String aggregateId) {
        super(aggregateId);
    }
}
```

### 2. Create Aggregate

```java
@EventSourced
public class OrderAggregate extends Aggregate {
    private String status;
    private String customerId;
    private BigDecimal total;

    public void create(String customerId, BigDecimal total) {
        apply(new OrderCreatedEvent(getId(), customerId, total));
    }

    public void ship() {
        if (!"PAID".equals(status)) {
            throw new IllegalStateException("Order must be paid first");
        }
        apply(new OrderShippedEvent(getId()));
    }

    @ApplyEvent
    private void on(OrderCreatedEvent event) {
        this.status = "CREATED";
        this.customerId = event.getCustomerId();
        this.total = event.getTotal();
    }

    @ApplyEvent
    private void on(OrderShippedEvent event) {
        this.status = "SHIPPED";
    }
}
```

### 3. Use Repository

```java
@Service
public class OrderService {
    
    private final AggregateRepository<OrderAggregate> orderRepository;

    public OrderService(EventStore eventStore, EventBus eventBus) {
        this.orderRepository = new AggregateRepository<>(eventStore, eventBus, OrderAggregate.class);
    }

    public String createOrder(String customerId, BigDecimal total) {
        OrderAggregate order = new OrderAggregate();
        order.create(customerId, total);
        orderRepository.save(order);
        return order.getId();
    }

    public void shipOrder(String orderId) {
        OrderAggregate order = orderRepository.load(orderId);
        order.ship();
        orderRepository.save(order);
    }
}
```

## Event Stores

| Store | Use Case |
|-------|----------|
| `InMemoryEventStore` | Testing, development |
| `JdbcEventStore` | Production with SQL database |
| `RedisEventStore` | Distributed, high-performance |

### JdbcEventStore Setup

```sql
CREATE TABLE event_store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data TEXT NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_aggregate_id (aggregate_id)
);
```

```java
@Bean
public EventStore eventStore(JdbcTemplate jdbcTemplate) {
    return new JdbcEventStore(jdbcTemplate);
}
```

## Async Event Bus

```java
@Bean
public EventBus eventBus() {
    return new AsyncEventBus(); // Non-blocking
}
```
