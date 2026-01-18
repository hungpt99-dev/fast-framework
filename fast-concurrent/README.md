# Fast Concurrent Module

A robust concurrency framework for Spring Boot applications, designed for resilience, observability, and ease of use.

## Features

- **Declarative Concurrency**: `@ConcurrentLimit`, `@ConcurrentSafe`, `@KeyedConcurrency`
- **Resilience**: Task timeouts, retries (with exponential backoff), fallbacks, and circuit breakers.
- **Parallel Flow**: Declarative engine for fan-out/fan-in operations (`ParallelFlow`).
- **Context Propagation**: Automatic propagation of MDC and Spring Security Context to worker threads.
- **Observability**: Built-in Micrometer metrics and SLF4J logging.
- **Executor Management**: Centralized `ExecutorRegistry` with configurable pools.

## Configuration

You can configure the thread pools and default behaviors in `application.properties` or `application.yml`:

```yaml
fast:
  concurrent:
    defaults:
      # Default permits for @ConcurrentSafe
      permits: 10
      # Default wait timeout for concurrency acquisition
      wait-timeout-ms: 200
      # Rejection policy: FAIL_FAST, WAIT_TIMEOUT, FALLBACK, WAIT
      reject-policy: WAIT_TIMEOUT
      
      # CPU-intensive executor configuration
      cpu:
        core-size: 10
        max-size: 100
        queue-capacity: 1000
        keep-alive-seconds: 60
        
      # I/O-intensive executor configuration
      io:
        core-size: 20
        max-size: 200
        queue-capacity: 2000
        keep-alive-seconds: 60
```

## Usage

### 1. Enable the Module
Add `@EnableFastConcurrent` to your main application class.

### 2. Annotations

```java
@Service
public class OrderService {

    // Global concurrency limit for this method
    @ConcurrentLimit(permits = 5)
    public void processHeavyTask() { ... }

    // Per-key concurrency limit (e.g., max 1 concurrent request per user)
    @KeyedConcurrency(key = "#userId", permits = 1)
    public void updateUser(String userId) { ... }
    
    // Offload to I/O pool
    @UseExecutor(ExecutorRegistry.IO)
    public Future<String> downloadFile() { ... }
}
```

### 3. Parallel Flow

```java
FlowResult result = ParallelFlow.of()
    .task("profile", () -> userService.getProfile(id))
    .task("orders", () -> orderService.getOrders(id))
    .timeout(Duration.ofSeconds(2))
    .failFast()
    .execute();
```
