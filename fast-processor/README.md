# Fast CQRS Processor

Compile-time annotation processor for zero-overhead CQRS implementations.

## Overview

This module provides annotation processors that generate implementation classes at **compile-time**, eliminating runtime reflection overhead. Instead of using JDK dynamic proxies that rely on reflection for every method call, the generated code directly invokes the underlying operations.

## Key Benefits

| Feature | Runtime Proxy | APT Generated |
|---------|---------------|---------------|
| Method Dispatch | ~100+ ns | ~1 ns |
| Reflection Calls | Per request | Zero |
| GraalVM Native | Requires config | Works out of box |
| Debugging | Proxy stack traces | Normal stack traces |

## Processors

### 1. SqlRepositoryProcessor

Processes `@SqlRepository` interfaces and generates implementations for `@Select` and `@Execute` methods.

**Input:**
```java
@SqlRepository
public interface OrderRepository {
    
    @Select("SELECT * FROM orders WHERE id = :id")
    Optional<Order> findById(@Param("id") String id);
    
    @Execute("INSERT INTO orders (id, total) VALUES (:id, :total)")
    void insert(@Param("id") String id, @Param("total") BigDecimal total);
}
```

**Generated:**
```java
@Generated("com.fast.cqrs.processor.sql.SqlRepositoryProcessor")
public final class OrderRepository_FastImpl implements OrderRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ResultMapper resultMapper;
    
    public OrderRepository_FastImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.resultMapper = new ResultMapper();
    }
    
    @Override
    public Optional<Order> findById(String id) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        List<?> results = jdbcTemplate.query(
            "SELECT * FROM orders WHERE id = :id",
            params,
            resultMapper.getRowMapper(Order.class)
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public void insert(String id, BigDecimal total) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("total", total);
        jdbcTemplate.update("INSERT INTO orders (id, total) VALUES (:id, :total)", params);
    }
}
```

### 2. HttpControllerProcessor

Processes `@HttpController` interfaces and generates Spring MVC controller implementations.

**Input:**
```java
@HttpController
@RequestMapping("/orders")
public interface OrderController {
    
    @Query(handler = GetOrderHandler.class)
    @GetMapping("/{id}")
    OrderDto getOrder(@PathVariable String id, @ModelAttribute GetOrderQuery query);
    
    @Command(handler = CreateOrderHandler.class)
    @PostMapping
    void createOrder(@RequestBody CreateOrderCmd cmd);
}
```

**Generated:**
```java
@Generated("com.fast.cqrs.processor.controller.HttpControllerProcessor")
@RestController
@RequestMapping("/orders")
public final class OrderController_FastImpl implements OrderController {
    
    private final QueryBus queryBus;
    private final CommandBus commandBus;
    
    public OrderController_FastImpl(QueryBus queryBus, CommandBus commandBus) {
        this.queryBus = queryBus;
        this.commandBus = commandBus;
    }
    
    @Override
    @GetMapping("/{id}")
    public OrderDto getOrder(@PathVariable String id, @ModelAttribute GetOrderQuery query) {
        return queryBus.dispatch(query);
    }
    
    @Override
    @PostMapping
    public void createOrder(@RequestBody CreateOrderCmd cmd) {
        commandBus.dispatch(cmd);
    }
}
```

## Usage

### With Gradle

```gradle
dependencies {
    // Option 1: Use the starter (recommended)
    implementation 'com.fast:fast-starter:1.0.0'
    annotationProcessor 'com.fast:fast-processor:1.0.0'
    
    // Option 2: Add individually
    annotationProcessor project(':fast-processor')
}
```

### Fallback Behavior

The framework automatically prefers generated implementations when available:

1. During startup, `SqlRepositoryImportRegistrar` checks for `*_FastImpl` classes
2. If found → Uses generated class (zero-overhead)
3. If not found → Falls back to runtime proxy (existing behavior)

This means:
- **Development**: Works without running annotation processor
- **Production**: Gets full performance with APT-generated classes

## Generated Class Location

Generated classes are placed in:
- `build/generated/sources/annotationProcessor/java/main/`

They appear in the same package as the source interface.

## Compiler Options

Enable APT debugging with:

```gradle
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ['-Xlint:processing', '-Afast.debug=true']
}
```

## Limitations

Current implementation limitations (to be addressed in future versions):

1. **CRUD Methods**: `FastRepository` inherited methods are stubbed (use runtime proxy)
2. **Pageable**: Pagination parameters not yet fully supported
3. **Security**: `@PreAuthorize` integration pending
4. **Validation**: `@Valid` integration pending

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Compile-Time (APT)                           │
├─────────────────────────────────────────────────────────────────┤
│  @SqlRepository ────→ SqlRepositoryProcessor                    │
│                              │                                  │
│                              ▼                                  │
│                    OrderRepository_FastImpl.java                │
├─────────────────────────────────────────────────────────────────┤
│  @HttpController ───→ HttpControllerProcessor                   │
│                              │                                  │
│                              ▼                                  │
│                    OrderController_FastImpl.java                │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Runtime                                   │
├─────────────────────────────────────────────────────────────────┤
│  SqlRepositoryImportRegistrar                                   │
│       │                                                         │
│       ├─→ Try: ClassUtils.forName("*_FastImpl")                 │
│       │         └─→ ✓ Use generated class                       │
│       │                                                         │
│       └─→ Catch: ClassNotFoundException                         │
│                 └─→ Use runtime proxy (fallback)                │
└─────────────────────────────────────────────────────────────────┘
```
