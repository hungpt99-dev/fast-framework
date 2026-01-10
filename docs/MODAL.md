# Modal/DTO Module

## Purpose

The Modal module converts internal entity objects to **safe, controlled Maps** for API responses, event payloads, or inter-service communication.

---

## The Problem

When exposing entities directly, you risk:

- **Leaking sensitive data** (passwords, internal IDs)
- **Tight coupling** between domain model and API contract
- **Unexpected field exposure** when entities change
- **Inconsistent naming** across endpoints

---

## The Solution

Annotate entities with `@Modal` and control field exposure with `@ModalField`:

```java
@Modal
public class User {
    @ModalField
    private String id;              // ✓ Exposed
    
    @ModalField(name = "user_name")
    private String name;            // ✓ Exposed as "user_name"
    
    @ModalField(ignore = true)
    private String password;        // ✗ Never exposed
    
    private String internalNote;    // ✗ Not annotated = not exposed
}
```

---

## Pros and Cons

### ✅ Advantages

| Benefit | Explanation |
|---------|-------------|
| **Explicit exposure** | Only annotated fields are included |
| **Field renaming** | API naming independent from code |
| **Format control** | Date/number formatting built-in |
| **Nested support** | Nested objects converted recursively |
| **No boilerplate** | No manual mapping code needed |

### ⚠️ Trade-offs

| Trade-off | Mitigation |
|-----------|------------|
| Reflection overhead | Metadata cached for performance |
| Annotations on entities | Better than manual DTO mapping |
| Limited to Maps | Convert to JSON at serialization layer |

---

## When to Use

**Good for:**
- API responses with sensitive data concerns
- Event payloads where schema control matters
- Internal DTOs for service communication

**Consider alternatives for:**
- High-performance paths (use manual mapping)
- Complex transformations (use dedicated mappers)

---

## Usage

### Basic Conversion

```java
User user = userRepository.findById(id);
Map<String, Object> dto = ModalMapper.toMap(user);
```

### Field Options

| Option | Purpose | Example |
|--------|---------|---------|
| `name` | Rename in output | `@ModalField(name = "user_id")` |
| `ignore` | Skip this field | `@ModalField(ignore = true)` |
| `format` | Date/number format | `@ModalField(format = "yyyy-MM-dd")` |

### Nested Objects

```java
@Modal
public class Order {
    @ModalField
    private String id;
    
    @ModalField
    private Customer customer;  // Nested @Modal
    
    @ModalField
    private List<OrderItem> items;  // Collection of @Modal
}
```

Nested objects with `@Modal` are converted recursively.

### Date Formatting

```java
@ModalField(format = "yyyy-MM-dd")
private LocalDate birthDate;  // "2000-01-15"

@ModalField(format = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createdAt;  // "2026-01-10 12:30:45"
```

---

## Best Practices

| Practice | Reason |
|----------|--------|
| Always add `@ModalField(ignore = true)` to sensitive fields | Security |
| Use explicit `name` for public API fields | API contract stability |
| Keep `@Modal` entities focused | Don't mix domain logic |
| Test output format in integration tests | Catch accidental exposure |
