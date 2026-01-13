# Fast Framework

This internal framework is built on top of **Spring Boot** with the goal of **standardizing code quality across teams with different experience levels**, while keeping the system **explicit, predictable, and easy to maintain**.

The framework focuses on **CQRS-first design**, enforcing a clear separation between **read** and **write** logic, and intentionally **avoids ORM “magic” behaviors** that often hide performance costs and introduce subtle bugs.

## Core Goals

1. **Ensure consistent code quality**
   * Enforce conventions and structure regardless of developer seniority
   * Reduce architectural drift across teams and services
   * Make code review simpler and more objective

2. **Avoid hidden ORM behavior**
   * No implicit lazy loading
   * No accidental N+1 queries
   * Explicit queries, explicit transactions, explicit mapping
   * Database access should be easy to reason about and easy to profile

3. **Reduce boilerplate without losing clarity**
   * Remove repetitive glue code (handlers, controllers, mappers)
   * Keep business logic explicit and visible
   * Generated code is readable and modifiable

4. **Make common cases easy, uncommon cases possible**
   * 80% of use cases require minimal configuration
   * Advanced scenarios remain fully customizable
   * No “framework lock-in” for edge cases

5. **Lower the learning curve**
   * Clear mental model: *Command → Handler → State change* / *Query → Handler → Read model*
   * Minimal annotations
   * Predictable execution flow

---

## Design Principles

### 1. CQRS by Default
* Commands and Queries are **first-class citizens**
* No mixed read/write handlers
* Read models are optimized for queries, not reused domain entities

### 2. Explicit Over Implicit
* No magic entity state tracking
* No automatic cascading writes
* No hidden database calls
* What you see in code is what actually happens at runtime

### 3. Convention Over Configuration
* Strong package and naming conventions
* Automatic wiring based on intent, not reflection tricks
* Convention violations fail fast at startup

### 4. Framework as Guardrail, Not Abstraction
* The framework **guides** developers instead of hiding complexity
* You can always drop down to plain Spring / JDBC when needed
* No custom DSL that replaces Java or Spring idioms

---

## Capabilities

### What the Framework Handles for You
* Command / Query routing and handler discovery
* Transaction boundary enforcement
* Consistent exception handling
* Logging, tracing, and metrics hooks
* Mapping between persistence models and DTOs
* Boilerplate controller generation
* Compile-time or startup-time convention checks

### What the Framework Does NOT Do
* No ORM state magic
* No hidden async behavior
* No automatic retries without visibility

---

## Quick Start

```groovy
dependencies {
    implementation 'com.fast:fast-starter:1.0.0-SNAPSHOT'
}
```

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

## Technical Guide

### CQRS Controllers

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

### SQL Repositories

The framework uses a direct-to-SQL approach to avoid ORM complexity.

```java
@SqlRepository
public interface OrderRepository extends FastRepository<Order, String> {
    // Auto: findById, findAll, save, saveAll, deleteById, etc.

    @Select("SELECT * FROM orders WHERE customer_id = :customerId")
    List<Order> findByCustomerId(@Param("customerId") String customerId);
}
```





### Concurrency Utilities

```java
import com.fast.cqrs.concurrent.task.*;
import com.fast.cqrs.concurrent.flow.*;

// Task with timeout, retry, fallback
User user = Tasks.supply("load-user", () -> userService.load(id))
    .timeout(2, TimeUnit.SECONDS)
    .retry(3)
    .fallback(() -> User.EMPTY)
    .execute();
```

---

## Module Overview

| Module | Purpose |
|--------|---------|
| `fast-starter` | All-in-one dependency (Recommended) |
| `fast-core` | Core CQRS patterns (Command/Query buses) |
| `fast-sql` | explicit SQL repositories |
| `fast-dx` | Developer Experience (CLI, convention enforcement) |
| `fast-logging` | Observability (Logging, tracing) |
| `fast-concurrent`| Virtual Thread structured concurrency |
| `fast-util` | Common utilities |

---

## Requirements

- Java 25+
- Spring Boot 4.0+
- Gradle 9.x

---

## Documentation Links

- [CQRS.md](docs/CQRS.md) - Controllers and handlers
- [SQL.md](docs/SQL.md) - SQL repositories
- [REPOSITORY.md](docs/REPOSITORY.md) - FastRepository
- [EXAMPLE.md](docs/EXAMPLE.md) - Complete example

---

## License

Apache License 2.0
