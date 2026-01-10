# Architecture

## Overview

Fast Framework uses a **layered proxy architecture** where interfaces are implemented at runtime using JDK dynamic proxies. This eliminates boilerplate while maintaining type safety.

## Request Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP Request                            │
└─────────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│  TraceIdFilter                                                  │
│  - Generates unique trace ID                                    │
│  - Stores in MDC for all logs                                   │
│  - Logs HTTP method, path, status, duration                     │
└─────────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│  Controller Interface (Dynamic Proxy)                           │
│  - No implementation class needed                               │
│  - Proxied by ControllerInvocationHandler                       │
└─────────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│  CqrsDispatcher                                                 │
│  - Reads @Query or @Command annotation                          │
│  - Validates method has exactly one annotation                  │
│  - Routes to appropriate bus                                    │
└─────────────────────────────────────────────────────────────────┘
                    ↓                           ↓
        ┌───────────────────┐       ┌───────────────────┐
        │     QueryBus      │       │    CommandBus     │
        │  - Finds handler  │       │  - Finds handler  │
        │  - Returns result │       │  - Void return    │
        └───────────────────┘       └───────────────────┘
                    ↓                           ↓
        ┌───────────────────┐       ┌───────────────────┐
        │   QueryHandler    │       │  CommandHandler   │
        │  - Business logic │       │  - Business logic │
        │  - Injects repos  │       │  - Injects repos  │
        └───────────────────┘       └───────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│  SQL Repository Interface (Dynamic Proxy)                       │
│  - Reads @Select or @Execute annotation                         │
│  - Binds @Param parameters                                      │
│  - Executes via NamedParameterJdbcTemplate                      │
└─────────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│                          Database                               │
└─────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| `TraceIdFilter` | Request tracing, MDC management |
| `ControllerInvocationHandler` | Proxies controller interfaces |
| `CqrsDispatcher` | Routes to query/command bus |
| `QueryBus` / `CommandBus` | Finds and invokes handlers |
| `SqlRepositoryInvocationHandler` | Proxies repository interfaces |
| `SqlExecutor` | Executes SQL via JDBC template |

## Module Dependencies

```
fast-cqrs-util (standalone)
        ↑
fast-cqrs-modal (standalone)
        ↑
fast-cqrs-core
        ↑
fast-cqrs-autoconfigure ←── fast-cqrs-logging
        ↑
fast-cqrs-sql
```

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Interface-only controllers | Eliminates boilerplate, enforces thin controllers |
| Dynamic proxies over code generation | Runtime flexibility, no build plugins needed |
| Annotations over convention | Explicit intent, no hidden behavior |
| Spring JDBC over ORM | Predictable SQL, no entity lifecycle |
| MDC for tracing | Industry standard, works with any log aggregator |
