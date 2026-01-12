# Event Sourcing Module

Comprehensive event sourcing framework for Spring Boot applications.

## Installation

```groovy
implementation 'com.fast:fast-cqrs-eventsourcing:1.0.0-SNAPSHOT'
```

---

## 1. Aggregates

```java
@AggregateRoot
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

---

## 2. Repository

```java
AggregateRepository<OrderAggregate> repo = new AggregateRepository<>(
    eventStore, eventBus, OrderAggregate.class);

OrderAggregate order = repo.load(orderId);
order.ship();
repo.save(order);
```

---

## 3. Event Store

| Implementation | Use Case |
|----------------|----------|
| `InMemoryEventStore` | Testing |
| `JdbcEventStore` | Production |

---

## 4. Snapshots

```java
SnapshotStore snapshotStore = new JdbcSnapshotStore(jdbcTemplate);

// Auto-snapshotting via @AggregateRoot
@AggregateRoot(snapshotStrategy = SnapshotStrategy.EVENT_COUNT, snapshotEveryN = 100)
public class OrderAggregate extends Aggregate { ... }
```

---

## 5. Projections

```java
@Component
public class OrderSummaryProjection implements Projection {
    
    @Override
    public String getName() { return "order-summary"; }
    
    @ProjectionHandler
    public void on(OrderCreatedEvent event) {
        // Update read model
    }
}

// Register and process
ProjectionManager manager = new ProjectionManager();
manager.register(projection);
manager.process(event);
```

---

## 6. Replay

```java
ReplayEngine engine = new ReplayEngine(eventStore, projectionManager);

ReplayProgress progress = engine.replay("order-summary");

// Monitor
while (!progress.isComplete()) {
    System.out.println(progress.getPercentComplete() + "%");
}
```

---

## 7. Sagas

```java
public class OrderFulfillmentSaga extends Saga {
    
    @SagaEventHandler
    public void on(OrderCreatedEvent event) {
        // Coordinate workflow
    }
    
    @Override
    protected void onComplete() {
        // Saga finished
    }
}
```

---

## 8. Outbox Pattern

```java
OutboxPublisher publisher = new OutboxPublisher(outboxStore)
    .onPublish(entry -> kafkaProducer.send(entry))
    .maxRetries(3)
    .batchSize(100);

publisher.start();
```

---

## 9. Testing

```java
AggregateTestFixture<OrderAggregate> fixture = 
    AggregateTestFixture.create(OrderAggregate::new);

fixture
    .given(new OrderCreatedEvent("order-1", "customer-1"))
    .when(order -> order.ship())
    .then()
        .expectEvent(OrderShippedEvent.class)
        .expectState(o -> assertEquals("SHIPPED", o.getStatus()));
```

---

## API Reference

| Package | Components |
|---------|------------|
| `domain` | `EventMetadata`, `@AggregateRoot`, `@AggregateLifecycle` |
| `snapshot` | `Snapshot`, `SnapshotStore` |
| `projection` | `Projection`, `ProjectionManager` |
| `replay` | `ReplayEngine`, `ReplayProgress` |
| `saga` | `Saga`, `SagaStore` |
| `outbox` | `OutboxEntry`, `OutboxPublisher` |
| `test` | `AggregateTestFixture` |
| `metrics` | `EventSourcingMetrics` |
