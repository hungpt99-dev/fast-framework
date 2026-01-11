# Virtual Threads (Concurrent Module)

Utilities for parallel execution using Java 21+ Virtual Threads.

## Installation

```groovy
implementation 'com.fast:fast-cqrs-concurrent:1.0.0-SNAPSHOT'
```

## Usage

```java
import static com.fast.cqrs.concurrent.VirtualThread.*;
```

## Features

### Basic Operations

```java
// Run and get result
var user = get(run(() -> userService.findById(id)));

// Fire and forget
execute(() -> notificationService.send(msg));
```

### Parallel Execution

```java
// Run all in parallel
var users = parallel(
    () -> fetchUser(1),
    () -> fetchUser(2),
    () -> fetchUser(3)
);

// Race - first to complete wins
var result = race(
    () -> fetchFromServer1(),
    () -> fetchFromServer2()
);
```

### Timeout

```java
// With timeout
var data = timeout(() -> slowService.call(), 5, TimeUnit.SECONDS);

// Shorthand (seconds)
var data = timeout(() -> slowService.call(), 5);
```

### Retry

```java
// Retry 3 times
var result = retry(() -> unreliableApi.call(), 3);

// With delay between retries
var result = retry(() -> api.call(), 3, 500); // 500ms delay
```

### Scheduling

```java
// Execute after delay
delay(1000, () -> log.info("After 1 second"));

// Periodic execution
interval(5000, () -> healthCheck());

// Sleep
sleep(100);
```

## API Reference

| Method | Description |
|--------|-------------|
| `run(Supplier)` | Submit task, returns Future |
| `execute(Runnable)` | Fire-and-forget |
| `get(Future)` | Block and get result |
| `parallel(Supplier...)` | Run all, return all results |
| `race(Supplier...)` | Return first completed |
| `timeout(Supplier, time)` | Fail if too slow |
| `retry(Supplier, attempts)` | Retry on failure |
| `delay(ms, Runnable)` | Execute after delay |
| `interval(ms, Runnable)` | Execute periodically |
| `sleep(ms)` | Sleep current thread |
