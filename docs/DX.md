# Developer Experience

## Zero Configuration

Just one annotation to enable everything:

```java
@SpringBootApplication
@EnableFast
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

That's it. No more:
- `@EnableCqrs`
- `@EnableSqlRepositories`
- Package configurations

---

## Project Structure Conventions

```
src/main/java/com/example/order/
├── OrderApplication.java       # @EnableFast
├── controller/
│   └── OrderController.java    # *Controller suffix
├── handler/
│   └── CreateOrderHandler.java # *Handler suffix
├── repository/
│   └── OrderRepository.java    # *Repository suffix
├── entity/
│   └── Order.java
├── dto/
│   ├── CreateOrderCmd.java     # *Cmd suffix
│   └── GetOrderQuery.java      # *Query suffix
├── event/
│   └── OrderCreatedEvent.java  # *Event suffix
└── aggregate/
    └── OrderAggregate.java     # *Aggregate suffix
```

---

## Naming Conventions

| Component | Package | Suffix | Example |
|-----------|---------|--------|---------|
| Controller | `*.controller` | `Controller` | `OrderController` |
| Handler | `*.handler` | `Handler` | `CreateOrderHandler` |
| Repository | `*.repository` | `Repository` | `OrderRepository` |
| Entity | `*.entity` | - | `Order` |
| Command | `*.dto` | `Cmd` | `CreateOrderCmd` |
| Query | `*.dto` | `Query` | `GetOrderQuery` |
| Event | `*.event` | `Event` | `OrderCreatedEvent` |
| Aggregate | `*.aggregate` | `Aggregate` | `OrderAggregate` |

---

## CLI Code Generator

Generate components instantly:

```bash
# Generate single component
java -cp fast-cli.jar com.fast.cqrs.cli.FastCli generate controller Order
java -cp fast-cli.jar com.fast.cqrs.cli.FastCli generate handler CreateOrder
java -cp fast-cli.jar com.fast.cqrs.cli.FastCli generate entity Order
java -cp fast-cli.jar com.fast.cqrs.cli.FastCli generate repository Order
java -cp fast-cli.jar com.fast.cqrs.cli.FastCli generate event OrderCreated
java -cp fast-cli.jar com.fast.cqrs.cli.FastCli generate aggregate Order

# Generate ALL components for a domain
java -cp fast-cli.jar com.fast.cqrs.cli.FastCli generate all Product
```

---

## Convention Validation

At startup, the framework logs warnings for convention violations:

```
WARN  - Controller 'com.example.service.MyController' violates naming conventions: 
        should be in '*.controller' package with 'Controller' suffix
```

Enable strict mode to fail on violations:

```java
@EnableFast(strict = true)
```

---

## Fast Startup

- Lazy initialization by default
- Convention-based scanning (no classpath scanning)
- Minimal reflection usage
- No bytecode generation at runtime
