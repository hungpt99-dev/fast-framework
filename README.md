# Fast CQRS Framework

A lightweight internal framework for CQRS, SQL repositories, event sourcing, and virtual threads in Java microservices.

---

## Quick Start

```groovy
dependencies {
    implementation 'com.fast:fast-cqrs-starter:1.0.0-SNAPSHOT'
}
```

```java
@SpringBootApplication
@EnableCqrs(basePackages = "com.example.controller")
@EnableSqlRepositories(basePackages = "com.example.repository")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## Features

| Module | Description |
|--------|-------------|
| `fast-cqrs-core` | CQRS annotations, buses, handlers |
| `fast-cqrs-sql` | SQL repositories with `FastRepository` |
| `fast-cqrs-event` | Event-driven infrastructure |
| `fast-cqrs-eventsourcing` | Full event sourcing support |
| `fast-cqrs-concurrent` | Virtual Thread utilities |
| `fast-cqrs-logging` | Automatic tracing and logging |
| `fast-cqrs-util` | Common utilities |

---

## CQRS Controllers

```java
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    @Query
    @GetMapping("/{id}")
    Order getOrder(@PathVariable String id);

    @Command
    @PostMapping
    void createOrder(@RequestBody CreateOrderCmd cmd);
}
```

---

## SQL Repositories

```java
@SqlRepository
public interface OrderRepository extends FastRepository<Order, String> {
    // Auto: findById, findAll, save, saveAll, deleteById, etc.

    @Select("SELECT * FROM orders WHERE customer_id = :customerId")
    List<Order> findByCustomerId(@Param("customerId") String customerId);
}
```

**Entity:**
```java
@Table("orders")
public class Order {
    @Id
    private String id;
    
    @Column("customer_id")
    private String customerId;
}
```

---

## Event-Driven

```java
// Publish events
@Autowired EventBus eventBus;
eventBus.publish(new OrderCreatedEvent(orderId));

// Handle events
@Component
public class OrderCreatedHandler implements DomainEventHandler<OrderCreatedEvent> {
    @Override
    public void handle(OrderCreatedEvent event) {
        // Send notification, update read model
    }
}
```

---

## Event Sourcing

```java
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

**Repository:**
```java
AggregateRepository<OrderAggregate> repo = new AggregateRepository<>(
    eventStore, eventBus, OrderAggregate.class);

OrderAggregate order = repo.load(orderId);
order.ship();
repo.save(order);
```

---

## Virtual Threads

```java
import static com.fast.cqrs.concurrent.VirtualThread.*;

// Parallel execution
var users = parallel(
    () -> fetchUser(1),
    () -> fetchUser(2)
);

// Timeout
var data = timeout(() -> slowService.call(), 5, TimeUnit.SECONDS);

// Retry
var result = retry(() -> unreliableApi.call(), 3);
```

---

## Modules

| Module | When to Use |
|--------|-------------|
| `fast-cqrs-starter` | All-in-one (recommended) |
| `fast-cqrs-core` | Just CQRS controllers |
| `fast-cqrs-sql` | SQL repositories |
| `fast-cqrs-event` | Lightweight events only |
| `fast-cqrs-eventsourcing` | Full event sourcing |
| `fast-cqrs-concurrent` | Virtual Thread utilities |

---

## Requirements

- Java 21+
- Spring Boot 3.2+
- Gradle 8.x

---

## Documentation

- [CQRS.md](docs/CQRS.md) - Controllers and handlers
- [SQL.md](docs/SQL.md) - SQL repositories
- [REPOSITORY.md](docs/REPOSITORY.md) - FastRepository
- [EVENTS.md](docs/EVENTS.md) - Event sourcing
- [EXAMPLE.md](docs/EXAMPLE.md) - Complete example

---

## License

Apache License 2.0
