# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0-SNAPSHOT] - 2026-01-10

### Added

**fast-core**
- `@HttpController` annotation for interface-only controllers
- `@Query` and `@Command` annotations for CQRS enforcement
- `CommandBus` and `QueryBus` interfaces with default implementations
- `CommandHandler<C>` and `QueryHandler<Q, R>` interfaces
- `CqrsDispatcher` for routing and validation
- Dynamic proxy support via `ControllerProxyFactory`

**fast-autoconfigure**
- `@EnableCqrs` annotation for enabling framework
- Spring Boot 4 auto-configuration
- Classpath scanning for controller interfaces

**fast-modal**
- `@Modal` class-level annotation
- `@ModalField` field-level annotation with name, ignore, format options
- `ModalMapper` for entity to Map conversion
- Nested object and collection support
- Field metadata caching for performance

**fast-sql**
- `@SqlRepository` annotation for interface-only repositories
- `@Select` and `@Execute` SQL annotations
- `@Param` for named parameter binding
- Spring JDBC integration with `NamedParameterJdbcTemplate`
- `@EnableSqlRepositories` annotation

**fast-logging**
- `TraceIdFilter` for MDC trace context
- `@TraceLog` annotation for method timing
- `@Loggable` annotation for business events
- `FrameworkExceptionHandler` for global exception handling
- `SafeLogUtil` for sensitive data masking

**fast-util**
- `StringUtil` - String manipulation utilities
- `DateUtil` - Date/time utilities
- `IdGenerator` - ID generation utilities
- `Assert` - Validation assertions
- `Result<T>` - Success/failure type
- `CollectionUtil` - Collection utilities
- `JsonUtil` - JSON utilities (requires Jackson)

---

## [Unreleased]

### Planned
- Pagination support for SQL repositories
- Batch insert/update operations
- OpenTelemetry integration
- Compile-time SQL validation
