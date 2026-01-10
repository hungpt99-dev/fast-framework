# SQL Repository Module

## Purpose

The SQL Repository module provides **interface-only data access** with explicit SQL queries. Unlike ORMs, there is no magic - you write SQL, and the framework executes it.

---

## Why SQL-First?

### The Problem with ORMs

| ORM Issue | Impact |
|-----------|--------|
| Hidden queries | N+1 problems, unpredictable performance |
| Entity lifecycle | Confusion about attached vs detached states |
| Lazy loading | Unexpected database calls, session errors |
| Query generation | Complex joins become unreadable |
| Debugging difficulty | Stack traces through proxy layers |

### Our Solution

| Approach | Benefit |
|----------|---------|
| **Explicit SQL** | You see exactly what runs |
| **No entity state** | Objects are just data containers |
| **Named parameters** | Clear, readable bindings |
| **Spring JDBC** | Proven, lightweight foundation |

---

## Pros and Cons

### ✅ Advantages

- **Predictable performance** - No hidden queries
- **Easy to debug** - SQL is visible in code and logs
- **Full SQL power** - Use any database feature
- **Simple mental model** - No lifecycle to understand
- **Lightweight** - Minimal runtime overhead

### ⚠️ Trade-offs

- **More typing** - Must write SQL manually
- **No lazy loading** - Load what you need upfront
- **No cascading** - Handle relationships explicitly
- **SQL knowledge required** - Team must know SQL

---

## When to Use

**Ideal for:**
- Performance-critical applications
- Complex queries (joins, CTEs, window functions)
- Teams comfortable with SQL
- Microservices with simple data models

**Consider ORM instead for:**
- Rapid prototypes needing CRUD generation
- Complex object graphs with cascading needs
- Teams unfamiliar with SQL

---

## How It Works

1. **Define interface** with `@SqlRepository`
2. **Annotate methods** with `@Select` or `@Execute`
3. **Bind parameters** with `@Param`
4. **Framework creates proxy** using Spring JDBC

```
Method call → Proxy → NamedParameterJdbcTemplate → Database
```

---

## Usage

### Basic Repository

```java
@SqlRepository
public interface ProductRepository {

    @Select("SELECT * FROM products WHERE id = :id")
    Product findById(@Param("id") String id);

    @Select("SELECT * FROM products WHERE category = :cat")
    List<Product> findByCategory(@Param("cat") String category);

    @Execute("INSERT INTO products(id, name, price) VALUES (:id, :name, :price)")
    void insert(@Param("id") String id, 
                @Param("name") String name, 
                @Param("price") BigDecimal price);

    @Execute("DELETE FROM products WHERE id = :id")
    void delete(@Param("id") String id);
}
```

### Return Types

| Return Type | Behavior |
|-------------|----------|
| `T` | Single object (null if not found) |
| `List<T>` | List of results (empty if none) |
| `Optional<T>` | Optional wrapper |
| `void` | No return value |
| `int` | Number of rows affected |

### Complex Queries

```java
@Select("""
    SELECT o.id, o.total, c.name as customer_name
    FROM orders o
    JOIN customers c ON o.customer_id = c.id
    WHERE o.status = :status
    AND o.created_at > :since
    ORDER BY o.created_at DESC
    LIMIT :limit
    """)
List<OrderSummary> findRecentOrders(
    @Param("status") String status,
    @Param("since") LocalDateTime since,
    @Param("limit") int limit);
```

---

## Best Practices

| Practice | Reason |
|----------|--------|
| Use multi-line strings for complex SQL | Readability |
| Always use `@Param` | Explicit binding, no guessing |
| Return `Optional<T>` for nullable results | Avoid null checks |
| Name parameters clearly | Self-documenting code |
| Use DTOs for projections | Don't expose entities |
