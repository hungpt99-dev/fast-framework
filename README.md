# fast-framework

**Code fast. Run fast. Review fast.**

`fast-framework` is a Java framework built on top of **Spring Boot**, designed to help teams **move quickly without losing control**, **maintain consistent code quality**, and **scale development across developers with different experience levels**.

The framework is built around **CQRS as a first-class architectural principle**, deliberately avoids **ORM magic**, and favors **explicit, predictable behavior**.
The result is code that is **easy to write, easy to run, and easy to review**.

---

## Motivation

As projects grow and teams expand, the main problems are rarely syntax or tooling — they are **inconsistency**, **hidden behavior**, and **review friction**.

Common issues in large Spring-based codebases:

* Each service follows a slightly different structure
* Business logic is scattered across controllers, services, and repositories
* ORM behavior introduces hidden queries and side effects
* Reviews require deep domain knowledge just to verify correctness
* Junior developers can unintentionally break architectural boundaries

These problems slow teams down and increase risk.

`fast-framework` exists to **turn architecture into something enforceable**, not something remembered, and to make **reviews fast because structure and intent are obvious**.

---

## What “Fast” Means

### Code Fast

Developers should be able to:

* Create new features with minimal setup
* Follow conventions instead of inventing structure
* Focus on business logic instead of infrastructure wiring

`fast-framework` removes repetitive boilerplate and provides a clear execution model:

* Command → Handler → State change
* Query → Handler → Read model

---

### Run Fast

Applications built with `fast-framework` are designed to be:

* Performance-predictable
* Easy to profile
* Safe under load

This is achieved by:

* Explicit queries and transactions
* No lazy-loading or hidden database access
* Read models optimized for query patterns
* Clear separation between read and write paths

Performance is not accidental — it is visible in the code.

---

### Review Fast

Reviews should focus on **business correctness**, not on deciphering structure or guessing side effects.

`fast-framework` makes reviews faster by:

* Enforcing a single, predictable layout across services
* Making command and query intent explicit
* Reducing boilerplate so reviewers see real logic
* Preventing architectural violations before code reaches review
* Making side effects easy to spot

A reviewer should be able to understand **what changed** and **what it affects** in minutes, not hours.

---

## Core Design Principles

### CQRS by Default

* Commands and Queries are strictly separated
* Read logic never mutates state
* Write logic never returns complex read models
* Each handler has one responsibility

This separation improves clarity, performance, and testability.

---

### Explicit Over Implicit

The framework intentionally avoids:

* ORM state tracking
* Automatic cascading writes
* Hidden retries
* Implicit asynchronous behavior

If something happens, it must be visible in code.

---

### Convention Over Configuration

* Strong naming and package conventions
* Automatic wiring based on intent, not reflection tricks
* Convention violations fail fast at startup

This reduces configuration noise and prevents architectural drift.

---

### Guardrails, Not Abstractions

`fast-framework` does not hide Spring or Java.

Instead, it:

* Constrains how features are used
* Guides developers toward safe patterns
* Allows escape hatches when needed

You can always drop down to:

* Plain Spring
* JDBC
* Custom SQL

The framework never blocks advanced use cases.

---

## What fast-framework Provides

* Command and Query routing
* Handler discovery and validation
* Transaction boundaries for commands
* Read-only execution guarantees for queries
* Boilerplate reduction for controllers and handlers
* Consistent exception and error handling
* Standard logging, tracing, and metrics hooks
* Compile-time or startup-time convention checks

---

## What fast-framework Intentionally Avoids

* ❌ ORM magic
* ❌ Event sourcing (by default)
* ❌ Implicit messaging or async execution
* ❌ Heavy abstractions or DSLs
* ❌ Opinionated infrastructure dependencies

---

## Who This Framework Is For

* **Junior developers**
  → Can write correct code by following conventions

* **Mid-level developers**
  → Can move fast without breaking architecture

* **Senior developers**
  → Can enforce standards and spend time on real complexity

---

## Long-Term Vision

`fast-framework` is not about adding more features.

It is about:

* Making systems easier to reason about
* Making teams safer as they scale
* Turning architecture into executable rules
* Keeping codebases boring, predictable, and reviewable

---

## Summary

`fast-framework` exists to remove friction from the development lifecycle:

* Faster development
* Predictable runtime behavior
* Faster, safer reviews

Not clever.
Not magical.
Just **fast**.

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

## Performance

Verified benchmarks on local hardware (MacBook M3, H2 Database):

| Scenario | Throughput | Description |
|:---|:---|:---|
| **Raw Framework** | **~9,650 ops/sec** | Fast-path dispatch with full persistence. |
| **Gateway** | **~6,750 ops/sec** | Full lifecycle (Validation, Logging) with fluent API. |
| **Async** | **~3,740 ops/sec** | Virtual Threads (limited by DB Connection Pool contention). |

*Benchmarks run with Console Logging enabled and H2 File Persistence.*

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
| `fast-sql` | Explicit SQL repositories |
| `fast-processor` | APT Processor for generating implementations |
| `fast-autoconfigure` | Spring Boot Auto-configuration |
| `fast-dx` | Developer Experience (CLI, convention enforcement) |
| `fast-logging` | Observability (Logging, tracing) |
| `fast-concurrent`| Virtual Thread structured concurrency |
| `fast-util` | Common utilities |

---

## Requirements

- Java 21+
- Spring Boot 3.4+
- Gradle 8.x/9.x

---

## Documentation Links

- [CQRS.md](docs/CQRS.md) - Controllers and handlers
- [SQL.md](docs/SQL.md) - SQL repositories
- [REPOSITORY.md](docs/REPOSITORY.md) - FastRepository
- [EXAMPLE.md](docs/EXAMPLE.md) - Complete example

---

## License

Apache License 2.0
