# Fast CQRS Framework

A lightweight internal framework to simplify CQRS controllers, SQL repositories, logging, and utilities in Java microservices.

---

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Getting Started](#getting-started)
   - [Prerequisites](#prerequisites)
   - [Installation](#installation)
4. [Usage](#usage)
   - [Configuration](#configuration)
   - [Example Code](#example-code)
5. [Architecture](#architecture)
6. [Modules](#modules)
7. [Contributing](#contributing)
8. [Versioning](#versioning)
9. [License](#license)
10. [Contact / Support](#contact--support)

---

## Overview

This framework standardizes HTTP controllers, data access, logging, and utilities across all microservices. It reduces boilerplate, ensures consistency, and integrates seamlessly with Spring Boot 4.

### Problems It Solves

| Problem | Solution |
|---------|----------|
| Inconsistent controller patterns | Interface-only CQRS controllers |
| ORM complexity and hidden queries | Explicit SQL repositories |
| Scattered, duplicate logging | Unified logging with trace IDs |
| Repeated utility code | Shared utility module |

### Design Philosophy

- **Interface-first** - Define contracts, not implementations
- **SQL is the source of truth** - No ORM magic
- **CQRS by default** - Clear separation of reads and writes
- **Zero logging in business code** - Framework handles observability
- **Boring by design** - Predictable, debuggable, maintainable

---

## Features

| Feature | Description |
|---------|-------------|
| **CQRS Controllers** | Interface-only HTTP controllers with strict query/command separation |
| **SQL Repositories** | Explicit SQL execution with named parameters |
| **Modal/DTO Mapping** | Annotation-based entity to map conversion |
| **Unified Logging** | Automatic trace IDs, method timing, exception handling |
| **Utilities** | Common helpers for strings, dates, IDs, assertions |

---

## Getting Started

### Prerequisites

- Java 25+
- Spring Boot 4.0+
- Gradle 9.x

### Installation

Add to your `build.gradle`:

**Option 1: All-in-One (Recommended)**

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.fast:fast-cqrs-starter:1.0.0-SNAPSHOT'
}
```

This single dependency includes all framework modules plus Spring Boot Web and JDBC starters.

**Option 2: Individual Modules**

If you need only specific modules:

```groovy
dependencies {
    implementation 'com.fast:fast-cqrs-autoconfigure:1.0.0-SNAPSHOT'  // Core CQRS
    implementation 'com.fast:fast-cqrs-sql:1.0.0-SNAPSHOT'            // SQL Repository
    implementation 'com.fast:fast-cqrs-logging:1.0.0-SNAPSHOT'        // Logging
    implementation 'com.fast:fast-cqrs-modal:1.0.0-SNAPSHOT'          // Modal/DTO
    implementation 'com.fast:fast-cqrs-util:1.0.0-SNAPSHOT'           // Utilities
}
```

---

## Usage

### Configuration

Enable the framework in your Spring Boot application:

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

### Example Code

**Define a controller interface:**

```java
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    @Query
    @GetMapping("/{id}")
    OrderDto getOrder(@PathVariable String id);

    @Command
    @PostMapping
    void createOrder(@RequestBody CreateOrderCmd cmd);
}
```

**Implement a handler:**

```java
@Component
public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderDto> {
    
    @Override
    public OrderDto handle(GetOrderQuery query) {
        return repository.findById(query.id());
    }
}
```

**Define a SQL repository:**

```java
@SqlRepository
public interface OrderRepository {

    @Select("SELECT * FROM orders WHERE id = :id")
    OrderDto findById(@Param("id") String id);

    @Execute("INSERT INTO orders(id, total) VALUES (:id, :total)")
    void insert(@Param("id") String id, @Param("total") BigDecimal total);
}
```

---

## Architecture

```
HTTP Request
    ↓
TraceIdFilter (creates trace context)
    ↓
Interface Controller (dynamic proxy)
    ↓
CQRS Dispatcher
    ├── @Query → QueryBus → QueryHandler
    └── @Command → CommandBus → CommandHandler
          ↓
SQL Repository (dynamic proxy)
    ↓
NamedParameterJdbcTemplate
    ↓
Database
```

**Key Principles:**
- Controllers are interfaces, proxied at runtime
- Handlers contain all business logic
- Repositories execute explicit SQL
- Logging is automatic, not in business code

See [docs/architecture.md](docs/architecture.md) for detailed diagrams.

---

## Modules

| Module | Description | Documentation |
|--------|-------------|---------------|
| `fast-cqrs-core` | CQRS annotations, buses, handlers | [CQRS.md](docs/CQRS.md) |
| `fast-cqrs-autoconfigure` | Spring Boot auto-configuration | - |
| `fast-cqrs-sql` | SQL repository framework | [SQL.md](docs/SQL.md) |
| `fast-cqrs-modal` | Entity to DTO mapping | [MODAL.md](docs/MODAL.md) |
| `fast-cqrs-logging` | Logging and tracing | [LOGGING.md](docs/LOGGING.md) |
| `fast-cqrs-util` | Common utilities | [UTIL.md](docs/UTIL.md) |

---

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/your-feature`
3. Commit changes: `git commit -m 'Add new feature'`
4. Push branch: `git push origin feature/your-feature`
5. Open a pull request

See [docs/contributing.md](docs/contributing.md) for guidelines.

---

## Versioning

We use [SemVer](https://semver.org/):

- `MAJOR` - Breaking changes
- `MINOR` - New features (backward compatible)
- `PATCH` - Bug fixes

See [docs/changelog.md](docs/changelog.md) for release history.

---

## License

Apache License 2.0

---

## Contact / Support

- **Maintainers:** Internal Framework Team
- **Issues:** Create an issue in the repository
