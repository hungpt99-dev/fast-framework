# CQRS Controller Module

## Purpose

The CQRS (Command Query Responsibility Segregation) module enforces a clean separation between:

- **Queries** - Read-only operations that return data
- **Commands** - State-changing operations that modify data

This separation makes code easier to test, optimize, and reason about.

---

## Why Use CQRS?

### ✅ Benefits

| Benefit | Explanation |
|---------|-------------|
| **Clear intent** | Method annotations explicitly state read vs write |
| **Testable handlers** | Business logic isolated from HTTP concerns |
| **Thin controllers** | No business logic in the HTTP layer |
| **Independent scaling** | Read and write paths can be optimized separately |
| **Audit-friendly** | Commands can be easily logged and replayed |

### ⚠️ Trade-offs

| Trade-off | Mitigation |
|-----------|------------|
| More classes (handlers, queries, commands) | Use records for simple DTOs |
| Learning curve for new developers | Consistent patterns reduce long-term confusion |
| Overkill for simple CRUD | Use traditional MVC for simple cases |

---

## How It Works

1. **Define interface** - Controller is just an interface with annotations
2. **Framework creates proxy** - Dynamic proxy implements the interface
3. **Dispatcher routes** - Based on @Query or @Command annotation
4. **Handler executes** - Business logic in a testable handler class

```
@Query → QueryBus → QueryHandler → returns data
@Command → CommandBus → CommandHandler → modifies state
```

---

## When to Use

**Good for:**
- Services with distinct read/write patterns
- Complex business logic requiring testability
- Services needing clear audit trails
- Teams standardizing architecture

**Consider alternatives for:**
- Simple CRUD operations
- Rapid prototypes
- One-off scripts

---

## Usage

### Step 1: Define Controller Interface

```java
@HttpController
@RequestMapping("/api/users")
public interface UserController {

    @Query
    @GetMapping("/{id}")
    UserDto getUser(@PathVariable String id);

    @Command
    @PostMapping
    void createUser(@RequestBody CreateUserCmd cmd);
}
```

**Key points:**
- `@HttpController` marks this as a controller interface
- Every method must have `@Query` or `@Command`
- No implementation class needed

### Step 2: Define Query/Command Records

```java
public record GetUserQuery(String id) {}
public record CreateUserCmd(String name, String email) {}
```

### Step 3: Implement Handlers

```java
@Component
public class GetUserHandler implements QueryHandler<GetUserQuery, UserDto> {
    
    private final UserRepository repository;
    
    @Override
    public UserDto handle(GetUserQuery query) {
        return repository.findById(query.id());
    }
}

@Component
public class CreateUserHandler implements CommandHandler<CreateUserCmd> {
    
    private final UserRepository repository;
    
    @Override
    public void handle(CreateUserCmd cmd) {
        repository.insert(cmd.name(), cmd.email());
    }
}
```

---

## Rules

| Rule | Reason |
|------|--------|
| Every method needs @Query or @Command | Framework enforces this at startup |
| No business logic in controllers | Controllers are just routing definitions |
| Handlers are Spring beans | Enables dependency injection and AOP |
| Commands return void | State changes should not return data |
