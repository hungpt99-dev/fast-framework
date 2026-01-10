# Utilities Module

## Purpose

Common utility classes to reduce boilerplate and improve code consistency across services.

---

## Why Use Framework Utilities?

| Reason | Benefit |
|--------|---------|
| **Consistent behavior** | Same null-handling, formatting across services |
| **Tested code** | Utilities are tested, your code doesn't need to retest |
| **Readable code** | `StringUtil.hasText(s)` vs `s != null && !s.isBlank()` |
| **Less dependencies** | Avoid adding Guava/Apache Commons for simple tasks |

---

## Available Utilities

| Class | Purpose |
|-------|---------|
| `StringUtil` | String manipulation and validation |
| `DateUtil` | Date formatting and parsing |
| `IdGenerator` | ID generation (UUID, time-based) |
| `Assert` | Input validation |
| `Result<T>` | Success/failure handling |
| `CollectionUtil` | Collection operations |
| `JsonUtil` | JSON serialization (requires Jackson) |

---

## StringUtil

For string validation and transformation.

```java
// Validation
StringUtil.isEmpty(str)    // null or ""
StringUtil.isBlank(str)    // null, "", or whitespace only
StringUtil.hasText(str)    // has meaningful content

// Transformation
StringUtil.truncate(str, 50)         // "Long text..."
StringUtil.toSnakeCase("firstName")  // "first_name"
StringUtil.toCamelCase("first_name") // "firstName"
StringUtil.mask("secret123", 2)      // "se***23"
StringUtil.defaultIfBlank(str, "N/A")
```

---

## DateUtil

For date operations without timezone confusion.

```java
// Current time
DateUtil.today()           // LocalDate.now()
DateUtil.now()             // LocalDateTime.now()
DateUtil.currentTimeMillis()

// Formatting
DateUtil.formatDate(date)      // "2026-01-10"
DateUtil.formatDateTime(dt)    // "2026-01-10 12:30:45"

// Parsing
DateUtil.parseDate("2026-01-10")
DateUtil.parseDateTime("2026-01-10 12:30:45")

// Utilities
DateUtil.isPast(date)
DateUtil.daysBetween(start, end)
```

---

## IdGenerator

For generating unique identifiers.

```java
IdGenerator.uuid()           // "a1b2c3d4e5f67890..."
IdGenerator.shortId()        // "a1b2c3d4"
IdGenerator.prefixedId("ORD")// "ORD-a1b2c3d4"
IdGenerator.random(12)       // "X7kM2pLq4Rst"
IdGenerator.timeBasedId()    // Sortable: "1704893421234_0001_a3f2"
```

**When to use which:**

| Method | Use Case |
|--------|----------|
| `uuid()` | Database primary keys |
| `shortId()` | User-facing codes |
| `prefixedId()` | Typed IDs (ORD-, USR-, TXN-) |
| `timeBasedId()` | When sorting by creation is needed |

---

## Assert

For input validation that throws `IllegalArgumentException`.

```java
Assert.notNull(user, "User is required");
Assert.hasText(name, "Name cannot be empty");
Assert.notEmpty(items, "Order must have items");
Assert.positive(count, "Count must be positive");
Assert.inRange(page, 1, 100, "Page out of range");
```

**Why use Assert:**
- Cleaner than if-throw blocks
- Consistent exception messages
- Fails fast at method entry

---

## Result<T>

For operations that can fail without throwing exceptions.

```java
// Creating results
Result<User> success = Result.success(user);
Result<User> failure = Result.failure("User not found");

// Using results
user = result.getOrThrow();        // throws if failure
user = result.getOrElse(default);  // returns default if failure

// Transforming
result.map(u -> u.getName())
result.flatMap(u -> findOrders(u.id()))

// Side effects
result
    .onSuccess(u -> log.info("Found user: {}", u))
    .onFailure(e -> log.warn("Lookup failed: {}", e));

// Wrapping risky operations
Result<Data> result = Result.of(() -> riskyDatabaseCall());
```

**When to use Result vs Exceptions:**

| Use Result | Use Exceptions |
|------------|----------------|
| Expected failures (not found, validation) | Unexpected failures (bugs, system errors) |
| Caller should handle the failure | Failure should propagate up |
| Multiple failure types in one method | Single failure mode |

---

## CollectionUtil

For common collection operations.

```java
// Null-safe checks
CollectionUtil.isEmpty(list)   // handles null
CollectionUtil.nullToEmpty(list)

// Access
CollectionUtil.first(list)     // first or null
CollectionUtil.last(list)      // last or null

// Transformation
CollectionUtil.toMap(users, User::getId)
CollectionUtil.partition(list, 100)  // chunks of 100
```

---

## JsonUtil

For JSON operations (requires Jackson on classpath).

```java
String json = JsonUtil.toJson(user);
User user = JsonUtil.fromJson(json, User.class);
String pretty = JsonUtil.toPrettyJson(user);
Map<String, Object> map = JsonUtil.toMap(user);
```

Note: `JsonUtil.isAvailable()` checks if Jackson is present.
