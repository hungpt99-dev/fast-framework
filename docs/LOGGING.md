# Logging Module

## Purpose

The Logging module provides **unified, zero-config observability** across all services:

- Automatic trace IDs in every log
- Method timing and slow detection
- Global exception handling (logged once)
- Safe logging with sensitive data masking

---

## The Problem

Without a unified logging approach:

| Issue | Impact |
|-------|--------|
| **Scattered trace IDs** | Can't correlate logs across services |
| **Duplicate exception logs** | Same error logged in every layer |
| **Inconsistent formats** | Hard to search and filter |
| **Logging in business code** | Cluttered, hard to maintain |
| **Sensitive data in logs** | Security and compliance risks |

---

## The Solution

| Feature | Implementation |
|---------|----------------|
| Trace context | `TraceIdFilter` adds ID to every request |
| Method timing | `@TraceLog` aspect measures execution |
| Business events | `@Loggable` for explicit logging |
| Exception handling | `FrameworkExceptionHandler` logs once |
| Safe logging | `SafeLogUtil` masks sensitive fields |

---

## Pros and Cons

### ✅ Advantages

| Benefit | Explanation |
|---------|-------------|
| **Log correlation** | Trace ID in every log entry |
| **No duplicate logs** | Exceptions logged exactly once |
| **Clean business code** | No logging statements needed |
| **Consistent format** | Same structure across services |
| **Auto slow detection** | Warns when methods exceed threshold |

### ⚠️ Trade-offs

| Trade-off | Mitigation |
|-----------|------------|
| Less control over logging | Framework handles common cases well |
| AOP overhead | Minimal impact, cached metadata |
| Must follow rules | Rules prevent common mistakes |

---

## How It Works

```
HTTP Request
    ↓
TraceIdFilter (creates traceId in MDC)
    ↓
Controller
    ↓
@TraceLog Aspect (measures time, never logs exceptions)
    ↓
Business Logic
    ↓
Exception (if any)
    ↓
FrameworkExceptionHandler (logs ONCE, returns error response)
```

---

## The Rules

| Rule | Reason |
|------|--------|
| **Never log and rethrow exceptions** | Causes duplicate logs |
| **Don't log exceptions in business code** | Global handler does it |
| **Don't log in loops** | Performance and log flooding |
| **Use @Loggable for business events** | Structured, intentional logging |

---

## Usage

### Method Timing (@TraceLog)

```java
@TraceLog(slowMs = 100)
public OrderDto getOrder(String id) {
    // Method execution time is logged
    // If > 100ms, logs as WARN
}
```

**Options:**

| Option | Default | Purpose |
|--------|---------|---------|
| `slowMs` | 500 | Threshold for slow warning |
| `logArgs` | false | Log method arguments |
| `logResult` | false | Log return value |

### Business Events (@Loggable)

```java
@Loggable("Creating order")
public void createOrder(CreateOrderCmd cmd) {
    // Logs: [BIZ] Creating order - OrderService.createOrder
}
```

Use for meaningful business events, not debugging.

---

## Log Categories

| Logger | Purpose | Level |
|--------|---------|-------|
| `FRAMEWORK_HTTP` | Request lifecycle | INFO |
| `FRAMEWORK_TRACE` | Method timing | DEBUG/WARN |
| `FRAMEWORK_BIZ` | Business events | INFO |
| `FRAMEWORK_EXCEPTION` | Unhandled errors | ERROR |

---

## Example Output

```
INFO  FRAMEWORK_HTTP [traceId=abc123] - GET /api/orders/1 200 45ms
DEBUG FRAMEWORK_TRACE [traceId=abc123] - -> OrderService.getOrder
DEBUG FRAMEWORK_TRACE [traceId=abc123] - <- OrderService.getOrder 23ms
WARN  FRAMEWORK_TRACE [traceId=abc123] - <- PaymentService.process SLOW 520ms
INFO  FRAMEWORK_BIZ [traceId=abc123] - [BIZ] Creating order - OrderService.create
ERROR FRAMEWORK_EXCEPTION [traceId=abc123] - Unhandled exception: NullPointerException
```

---

## Best Practices

| Practice | Reason |
|----------|--------|
| Set appropriate `slowMs` thresholds | Match your SLAs |
| Use `@Loggable` sparingly | Only for important events |
| Never catch-log-rethrow | Let global handler manage |
| Check log levels in production | DEBUG can be verbose |
