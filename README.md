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
```java
@SpringBootApplication
@EnableFast // Auto-configures everything based on conventions
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
| `fast-cqrs-core` | CQRS annotations, buses, handlers, events, event sourcing |
| `fast-cqrs-sql` | SQL repositories with `FastRepository` |
| `fast-cqrs-concurrent` | Virtual Thread utilities |
| `fast-cqrs-logging` | Automatic tracing and logging |
| `fast-cqrs-dx` | CLI code generator and convention validation |
| `fast-cqrs-util` | Common utilities |

---

## CQRS Controllers

```java
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    // Handler is optional - framework auto-dispatches based on Query type
    // Parameters are bound to GetOrderQuery automatically via @ModelAttribute
    @Query
    @GetMapping("/{id}")
    OrderDto getOrder(@PathVariable String id, @ModelAttribute GetOrderQuery query);

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

## Concurrency

```java
import com.fast.cqrs.concurrent.task.*;
import com.fast.cqrs.concurrent.flow.*;

// Task with timeout, retry, fallback
User user = Tasks.supply("load-user", () -> userService.load(id))
    .timeout(2, TimeUnit.SECONDS)
    .retry(3)
    .fallback(() -> User.EMPTY)
    .execute();

// Parallel execution
FlowResult result = ParallelFlow.of()
    .task("user", () -> loadUser())
    .task("orders", () -> loadOrders())
    .timeout(3, TimeUnit.SECONDS)
    .execute();
```

---

## Modules

| Module | When to Use |
|--------|-------------|
| `fast-cqrs-starter` | All-in-one (recommended) |
| `fast-cqrs-core` | CQRS, Events, Event Sourcing |
| `fast-cqrs-sql` | SQL repositories |
| `fast-cqrs-dx` | Developer Experience (CLI, Conventions) |
| `fast-cqrs-concurrent` | Virtual Thread utilities |

---

## Requirements

- Java 25+
- Spring Boot 4.0+
- Gradle 9.x

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
