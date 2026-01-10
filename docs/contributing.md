# Contributing

## How to Contribute

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/your-feature`
3. **Commit** changes: `git commit -m 'Add new feature'`
4. **Push** branch: `git push origin feature/your-feature`
5. **Open** a pull request

## Code Standards

### Java Style

- Use Java 25 features where appropriate
- Follow standard Java naming conventions
- Add Javadoc to all public methods
- Keep methods small and focused

### Commit Messages

Use conventional commits:

```
feat: add batch insert support for SQL repositories
fix: handle null values in ModalMapper
docs: update README with new examples
```

### Testing

- Add unit tests for new features
- Run `./gradlew test` before submitting PR
- Aim for >80% code coverage

## Module Guidelines

### Adding a new annotation

1. Create annotation in appropriate module (e.g., `fast-cqrs-core`)
2. Add corresponding aspect or handler
3. Update documentation
4. Add tests

### Adding a new utility method

1. Add to appropriate utility class (e.g., `StringUtil`, `DateUtil`)
2. Make methods static and null-safe
3. Keep dependencies minimal
4. Add tests and documentation

## Review Process

1. All PRs require at least one review
2. CI must pass (build, test)
3. Documentation must be updated for new features
