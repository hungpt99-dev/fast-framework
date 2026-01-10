# Troubleshooting

## Common Issues

### Controller methods not being intercepted

**Symptom:** HTTP endpoints work but CQRS dispatch not happening.

**Cause:** Missing `@EnableCqrs` or wrong base package.

**Solution:**
```java
@EnableCqrs(basePackages = "com.example.controller")  // Check this package
```

---

### "Method must be annotated with @Query or @Command"

**Symptom:** Exception at runtime when calling controller method.

**Cause:** Controller method missing required annotation.

**Solution:**
```java
@Query  // Add this
@GetMapping("/{id}")
OrderDto getOrder(@PathVariable String id);
```

---

### Repository method returns null unexpectedly

**Symptom:** @Select queries returning null when data exists.

**Cause:** Column names don't match property names.

**Solution:** Use aliases or ensure snake_case columns match camelCase properties:
```sql
SELECT user_id as userId, first_name as firstName FROM users
```

---

### Duplicate exception logs

**Symptom:** Same exception logged multiple times.

**Cause:** Logging exceptions in business code AND letting framework log.

**Solution:** Never catch-log-rethrow. Let `FrameworkExceptionHandler` log:
```java
// DON'T DO THIS
try {
    process();
} catch (Exception e) {
    log.error("Error", e);  // Remove this
    throw e;
}

// DO THIS
process();  // Let exceptions propagate naturally
```

---

### Trace ID not appearing in logs

**Symptom:** Logs missing `[traceId=...]`.

**Cause:** Using wrong log pattern.

**Solution:** Include MDC in log pattern:
```xml
<pattern>%d %level [traceId=%X{traceId}] - %msg%n</pattern>
```

---

### "Jackson is not on classpath" from JsonUtil

**Symptom:** Exception when using JsonUtil.

**Cause:** Jackson not included in dependencies.

**Solution:** Add Jackson dependency:
```groovy
implementation 'com.fasterxml.jackson.core:jackson-databind'
```

---

## FAQs

### Can I use JPA entities with @Modal?

Yes, but it's not recommended. @Modal works best with simple POJOs. JPA proxies may cause unexpected behavior.

### Do I need both @EnableCqrs and @EnableSqlRepositories?

Only if you're using both features. They are independent.

### How do I disable automatic exception logging?

Create your own `@ControllerAdvice` and exclude the framework's:
```java
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomExceptionHandler { ... }
```

### Can handlers call other handlers?

Not recommended. Use services for shared logic and inject those into handlers.
