# Security & Authorization

## Why Security Is Critical

Interface-only controllers are convenient, but without proper security:

| Risk | Consequence |
|------|-------------|
| No role check | Regular users can call admin handlers |
| No input validation | SQL injection, crashes, corrupted data |
| No audit trail | Cannot track who changed what and when |

**Example:** A normal user calling `CreateDiscountCodeCmd` without role check → security breach.

---

## 1. Role & Permission Annotations

### Using `@PreAuthorize`

The framework now supports Spring Security's `@PreAuthorize` on interface methods:

```java
@HttpController
@RequestMapping("/api/discounts")
public interface DiscountController {

    @PreAuthorize("hasRole('ADMIN')")
    @Command
    @PostMapping
    void createDiscount(@RequestBody CreateDiscountCmd cmd);

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Query
    @PostMapping("/list")
    List<DiscountDto> listDiscounts(@RequestBody ListDiscountsQuery query);

    @PreAuthorize("isAuthenticated()")
    @Query
    @PostMapping("/my-discounts")
    List<DiscountDto> getMyDiscounts(@RequestBody GetMyDiscountsQuery query);
}
```

### Supported Expressions

| Expression | Description |
|------------|-------------|
| `hasRole('ADMIN')` | User has ROLE_ADMIN |
| `hasAnyRole('ADMIN', 'USER')` | User has any of the roles |
| `hasAuthority('READ_PRIVILEGE')` | User has specific authority |
| `hasAnyAuthority('READ', 'WRITE')` | User has any of the authorities |
| `isAuthenticated()` | User is logged in |
| `permitAll()` | Allow everyone |
| `denyAll()` | Deny everyone |

### Setup

Add Spring Security to your application:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
```

Configure security:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    // Your security configuration
}
```

---

## 2. Input Validation

### Using Bean Validation

Add `@Valid` to validate request bodies:

```java
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    @Command
    @PostMapping
    void createOrder(@Valid @RequestBody CreateOrderCmd cmd);
}
```

Define validation rules in commands:

```java
public record CreateOrderCmd(
    @NotBlank(message = "Customer ID is required")
    String customerId,
    
    @NotEmpty(message = "At least one item is required")
    List<@Valid OrderItem> items,
    
    @Min(value = 0, message = "Discount cannot be negative")
    @Max(value = 100, message = "Discount cannot exceed 100%")
    Integer discountPercent
) {}

public record OrderItem(
    @NotBlank String productId,
    @Min(1) int quantity
) {}
```

### Common Validation Annotations

| Annotation | Purpose |
|------------|---------|
| `@NotNull` | Cannot be null |
| `@NotBlank` | Cannot be null or empty (strings) |
| `@NotEmpty` | Cannot be empty (collections) |
| `@Size(min, max)` | Length constraints |
| `@Min` / `@Max` | Numeric range |
| `@Email` | Valid email format |
| `@Pattern(regex)` | Custom regex pattern |

### Sanitization

For text input that will be displayed:

```java
public record CreateCommentCmd(
    @NotBlank
    @Size(max = 1000)
    @SafeHtml  // Prevents XSS
    String content
) {}
```

---

## 3. Audit Trail

### Approach 1: Handler-Level Logging

Log in your command handlers:

```java
@Component
public class CreateOrderHandler implements CommandHandler<CreateOrderCmd> {
    
    private final AuditLogger auditLogger;
    
    @Override
    public void handle(CreateOrderCmd cmd) {
        // Business logic
        Order order = orderRepository.save(cmd);
        
        // Audit log
        auditLogger.log(AuditEvent.builder()
            .action("CREATE_ORDER")
            .actor(SecurityContextHolder.getContext().getAuthentication().getName())
            .resource("Order")
            .resourceId(order.getId())
            .details(cmd)
            .build());
    }
}
```

### Approach 2: Command Bus Decorator

Wrap the CommandBus to automatically audit all commands:

```java
@Component
@Primary
public class AuditingCommandBus implements CommandBus {
    
    private final CommandBus delegate;
    private final AuditLogger auditLogger;
    
    @Override
    public <C> void dispatch(C command) {
        String actor = getCurrentUser();
        
        auditLogger.logBefore(command, actor);
        
        try {
            delegate.dispatch(command);
            auditLogger.logSuccess(command, actor);
        } catch (Exception e) {
            auditLogger.logFailure(command, actor, e);
            throw e;
        }
    }
}
```

### Approach 3: Database Audit Table

Store audit events in database:

```sql
CREATE TABLE audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actor       VARCHAR(255),
    action      VARCHAR(100),
    resource    VARCHAR(100),
    resource_id VARCHAR(255),
    details     JSON,
    ip_address  VARCHAR(50),
    success     BOOLEAN
);
```

---

## Security Checklist

Before deploying to production:

- [ ] All commands have appropriate `@PreAuthorize` annotations
- [ ] All request DTOs have validation annotations
- [ ] Sensitive operations have audit logging
- [ ] SQL repositories use parameterized queries (automatic in fast-sql)
- [ ] Error responses don't leak sensitive information
- [ ] CORS is properly configured
- [ ] Rate limiting is in place for public APIs

---

## See Also

- [CQRS.md](CQRS.md) - Controller basics
- [LOGGING.md](LOGGING.md) - Logging and tracing
