# Configuration

## Enable Annotations

### @EnableFast
 
Single annotation to enable all framework features (CQRS, SQL Repositories, etc.).
 
```java
@EnableFast
// OR with options
@EnableFast(basePackages = "com.example", strict = true)
```
 
| Attribute | Default | Description |
|-----------|---------|-------------|
| `basePackages` | Annotated class package | Packages to scan for conventions |
| `strict` | `false` | Throw exception on convention violations |
 
### Fine-Grained Annotations
 
If you prefer manual control, you can use these individual annotations instead of `@EnableFast`:
 
- `@EnableCqrs(basePackages = "...")`
- `@EnableSqlRepositories(basePackages = "...")`

---

## Logging Configuration

### Log Levels

Configure in `application.yml`:

```yaml
logging:
  level:
    FRAMEWORK_HTTP: INFO       # HTTP request/response
    FRAMEWORK_TRACE: DEBUG     # Method timing
    FRAMEWORK_BIZ: INFO        # Business events
    FRAMEWORK_EXCEPTION: ERROR # Unhandled exceptions
```

### Trace ID Header

The framework uses `X-Trace-Id` header. Incoming requests with this header will use the provided trace ID; otherwise, a new one is generated.

---

## DataSource Configuration

For SQL repositories, configure a standard Spring DataSource:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: pass
    driver-class-name: org.postgresql.Driver
```

---

## @TraceLog Defaults

| Option | Default | Description |
|--------|---------|-------------|
| `slowMs` | 500 | Threshold for WARN log |
| `logArgs` | false | Log method arguments |
| `logResult` | false | Log return value |

Override per method:

```java
@TraceLog(slowMs = 100, logArgs = true)
public OrderDto getOrder(String id) { ... }
```

---

## Auto-Configuration

All modules provide Spring Boot auto-configuration. Features are enabled automatically when:

| Module | Auto-enabled when |
|--------|-------------------|
| `fast-logging` | Web application context detected |
| `fast-sql` | DataSource bean present |
