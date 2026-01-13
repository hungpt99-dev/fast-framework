package com.fast.cqrs.dx.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * CLI Code Generator for Fast Framework.
 * <p>
 * Generates feature-based code structure (Clean Architecture):
 * 
 * <pre>
 * com.example.order/
 * ├── api/           # Controllers, DTOs
 * ├── domain/        # Entities, Aggregates, Events
 * ├── application/   # Handlers
 * └── infrastructure/ # Repositories
 * </pre>
 * <p>
 * Usage:
 * 
 * <pre>
 * fast-cli generate controller Order
 * fast-cli generate handler CreateOrder
 * fast-cli generate all Order
 * </pre>
 */
@Command(name = "fast-cli", mixinStandardHelpOptions = true, version = "1.0.0", description = "Fast Framework CLI - Code Generator and Developer Tools")
public class FastCli implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FastCli())
                .addSubcommand("generate", new GenerateCommand())
                .addSubcommand("g", new GenerateCommand())
                .execute(args);
        System.exit(exitCode);
    }
}

/**
 * Generate command for creating components.
 */
@Command(name = "generate", aliases = { "g" }, description = "Generate framework components (feature-based structure)")
class GenerateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Component type: controller, handler, entity, repository, dto, all")
    private String type;

    @Parameters(index = "1", description = "Feature/Component name (e.g., Order, CreateOrder)")
    private String name;

    @Option(names = { "-p",
            "--package" }, description = "Base package (default: com.example)", defaultValue = "com.example")
    private String basePackage;

    @Option(names = { "-o",
            "--output" }, description = "Output directory (default: src/main/java)", defaultValue = "src/main/java")
    private String outputDir;

    @Option(names = { "-f", "--feature" }, description = "Feature name (default: derived from component name)")
    private String featureName;

    @Override
    public Integer call() {
        try {
            // Derive feature name from component name if not specified
            String feature = featureName != null ? featureName : deriveFeatureName(name, type);

            FeatureGenerator generator = new FeatureGenerator(basePackage, outputDir, feature);

            switch (type.toLowerCase()) {
                case "controller" -> generator.generateController(name);
                case "handler" -> generator.generateHandler(name);
                case "entity" -> generator.generateEntity(name);
                case "repository" -> generator.generateRepository(name);

                case "dto" -> generator.generateDto(name);
                case "all" -> generator.generateAll(name);
                default -> {
                    System.err.println("Unknown type: " + type);
                    System.err.println(
                            "Valid types: controller, handler, entity, repository, dto, all");
                    return 1;
                }
            }
            return 0;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Derives feature name from component name.
     * e.g., "CreateOrderHandler" -> "order", "OrderController" -> "order"
     */
    private String deriveFeatureName(String name, String type) {
        String baseName = name;

        // Remove common suffixes
        String[] suffixes = { "Controller", "Handler", "Repository", "Cmd", "Query", "Dto" };
        for (String suffix : suffixes) {
            if (baseName.endsWith(suffix)) {
                baseName = baseName.substring(0, baseName.length() - suffix.length());
                break;
            }
        }

        // Remove common prefixes like "Create", "Update", "Delete", "Get"
        String[] prefixes = { "Create", "Update", "Delete", "Get", "Find", "List" };
        for (String prefix : prefixes) {
            if (baseName.startsWith(prefix) && baseName.length() > prefix.length()) {
                baseName = baseName.substring(prefix.length());
                break;
            }
        }

        return baseName.toLowerCase();
    }
}

/**
 * Feature-based component generator (Clean Architecture).
 * <p>
 * Structure:
 * 
 * <pre>
 * {basePackage}.{feature}/
 * ├── api/           # Controllers, DTOs (Cmd, Query, Response)
 * ├── domain/        # Entities, Value Objects
 * ├── application/   # Handlers (Command/Query handlers)
 * └── infrastructure/ # Repositories, External integrations
 * </pre>
 */
class FeatureGenerator {

    private final String basePackage;
    private final String outputDir;
    private final String feature;

    FeatureGenerator(String basePackage, String outputDir, String feature) {
        this.basePackage = basePackage;
        this.outputDir = outputDir;
        this.feature = feature.toLowerCase();
    }

    private String featurePackage() {
        return basePackage + "." + feature;
    }

    void generateController(String name) throws IOException {
        String className = ensureSuffix(name, "Controller");
        String entityName = removeSuffix(name, "Controller");

        String content = """
                package %s.api;

                import com.fast.cqrs.cqrs.annotation.HttpController;
                import com.fast.cqrs.cqrs.annotation.Query;
                import com.fast.cqrs.cqrs.annotation.Command;
                import org.springframework.web.bind.annotation.*;

                import java.util.List;

                /**
                 * REST API controller for %s feature.
                 * <p>
                 * Query endpoints use @ModelAttribute to bind query parameters.
                 * Handler is optional - if not specified, QueryBus auto-dispatches
                 * based on the query type.
                 */
                @HttpController
                @RequestMapping("/api/%s")
                public interface %s {

                    /**
                     * Get single %s by ID.
                     * Handler is optional - QueryBus will find handler by query type.
                     */
                    @Query
                    @GetMapping("/{id}")
                    %sDto get%s(@PathVariable String id, @ModelAttribute Get%sQuery query);

                    /**
                     * List %s with pagination and filters.
                     */
                    @Query
                    @GetMapping
                    List<%sDto> list%s(@ModelAttribute List%sQuery query);

                    @Command
                    @PostMapping
                    void create%s(@RequestBody Create%sCmd cmd);

                    @Command
                    @PutMapping("/{id}")
                    void update%s(@PathVariable String id, @RequestBody Update%sCmd cmd);

                    @Command
                    @DeleteMapping("/{id}")
                    void delete%s(@PathVariable String id);
                }
                """.formatted(
                featurePackage(),
                entityName,
                feature + "s",
                className,
                entityName,
                entityName, entityName, entityName,
                entityName,
                entityName, entityName, entityName,
                entityName, entityName,
                entityName, entityName,
                entityName);

        writeFile("api", className, content);
    }

    void generateHandler(String name) throws IOException {
        String className = ensureSuffix(name, "Handler");
        String commandName = removeSuffix(name, "Handler");

        String content = """
                package %s.application;

                import %s.api.%sCmd;
                import com.fast.cqrs.cqrs.CommandHandler;
                import org.springframework.stereotype.Component;

                /**
                 * Handler for %s command.
                 */
                @Component
                public class %s implements CommandHandler<%sCmd> {

                    @Override
                    public void handle(%sCmd cmd) {
                        // TODO: Implement business logic
                    }
                }
                """.formatted(
                featurePackage(),
                featurePackage(), commandName,
                commandName,
                className, commandName,
                commandName);

        writeFile("application", className, content);
    }

    void generateEntity(String name) throws IOException {
        String className = removeSuffix(name, "Entity");

        String content = """
                package %s.domain;

                import com.fast.cqrs.sql.repository.Id;
                import com.fast.cqrs.sql.repository.Table;
                import com.fast.cqrs.sql.repository.Column;

                /**
                 * %s entity.
                 */
                @Table("%ss")
                public class %s {

                    @Id
                    private String id;

                    @Column
                    private String name;

                    @Column
                    private String status;

                    public %s() {}

                    public %s(String id) {
                        this.id = id;
                    }

                    // Getters and Setters
                    public String getId() { return id; }
                    public void setId(String id) { this.id = id; }

                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }

                    public String getStatus() { return status; }
                    public void setStatus(String status) { this.status = status; }
                }
                """.formatted(
                featurePackage(),
                className,
                feature,
                className,
                className,
                className);

        writeFile("domain", className, content);
    }

    void generateRepository(String name) throws IOException {
        String entityName = removeSuffix(name, "Repository");
        String className = entityName + "Repository";

        String content = """
                package %s.infrastructure;

                import %s.domain.%s;
                import com.fast.cqrs.sql.annotation.SqlRepository;
                import com.fast.cqrs.sql.repository.FastRepository;

                import java.util.List;

                /**
                 * Repository for %s entity.
                 */
                @SqlRepository
                public interface %s extends FastRepository<%s, String> {

                    List<%s> findByStatus(String status);
                }
                """.formatted(
                featurePackage(),
                featurePackage(), entityName,
                entityName,
                className, entityName,
                entityName);

        writeFile("infrastructure", className, content);
    }

    void generateDto(String name) throws IOException {
        String baseName = name;

        // Generate Command DTO
        String cmdClassName = ensureSuffix(baseName, "Cmd");
        String cmdContent = """
                package %s.api;

                import jakarta.validation.constraints.NotBlank;

                /**
                 * Command DTO for %s.
                 */
                public record %s(
                    @NotBlank String name
                ) {}
                """.formatted(featurePackage(), baseName, cmdClassName);
        writeFile("api", cmdClassName, cmdContent);

        // Generate Response DTO
        String dtoClassName = removeSuffix(baseName, "Cmd") + "Dto";
        if (!dtoClassName.equals(cmdClassName)) {
            String dtoContent = """
                    package %s.api;

                    /**
                     * Response DTO for %s.
                     */
                    public record %s(
                        String id,
                        String name,
                        String status
                    ) {}
                    """.formatted(featurePackage(), baseName, dtoClassName);
            writeFile("api", dtoClassName, dtoContent);
        }
    }

    void generateQuery(String name) throws IOException {
        String queryClassName = ensureSuffix(name, "Query");
        String queryContent = """
                package %s.api;

                /**
                 * Query DTO for %s.
                 * Use @ModelAttribute to bind query parameters.
                 */
                public record %s(
                    Integer page,
                    Integer size,
                    String sort,
                    String filter
                ) {
                    public %s {
                        if (page == null) page = 0;
                        if (size == null) size = 20;
                    }
                }
                """.formatted(featurePackage(), name, queryClassName, queryClassName);
        writeFile("api", queryClassName, queryContent);
    }

    void generateQueryHandler(String name) throws IOException {
        String className = ensureSuffix(name, "Handler");
        String queryName = removeSuffix(name, "Handler");
        String entityName = deriveEntityName(queryName);

        String content = """
                package %s.application;

                import %s.api.%sQuery;
                import %s.api.%sDto;
                import %s.infrastructure.%sRepository;
                import com.fast.cqrs.cqrs.QueryHandler;
                import org.springframework.stereotype.Component;

                /**
                 * Handler for %s query.
                 */
                @Component
                public class %s implements QueryHandler<%sQuery, %sDto> {

                    private final %sRepository repository;

                    public %s(%sRepository repository) {
                        this.repository = repository;
                    }

                    @Override
                    public %sDto handle(%sQuery query) {
                        // TODO: Implement query logic
                        return null;
                    }
                }
                """.formatted(
                featurePackage(),
                featurePackage(), queryName,
                featurePackage(), entityName,
                featurePackage(), entityName,
                queryName,
                className, queryName, entityName,
                entityName,
                className, entityName,
                entityName, queryName);

        writeFile("application", className, content);
    }

    void generateListQueryHandler(String name) throws IOException {
        String className = "List" + name + "Handler";
        String queryName = "List" + name;

        String content = """
                package %s.application;

                import %s.api.%sQuery;
                import %s.api.%sDto;
                import %s.infrastructure.%sRepository;
                import com.fast.cqrs.cqrs.QueryHandler;
                import org.springframework.stereotype.Component;

                import java.util.List;

                /**
                 * Handler for %s query.
                 */
                @Component
                public class %s implements QueryHandler<%sQuery, List<%sDto>> {

                    private final %sRepository repository;

                    public %s(%sRepository repository) {
                        this.repository = repository;
                    }

                    @Override
                    public List<%sDto> handle(%sQuery query) {
                        // TODO: Implement query logic
                        return List.of();
                    }
                }
                """.formatted(
                featurePackage(),
                featurePackage(), queryName,
                featurePackage(), name,
                featurePackage(), name,
                queryName,
                className, queryName, name,
                name,
                className, name,
                name, queryName);

        writeFile("application", className, content);
    }

    private String deriveEntityName(String queryName) {
        // Remove prefixes like Get, List, Find
        String[] prefixes = { "Get", "List", "Find" };
        for (String prefix : prefixes) {
            if (queryName.startsWith(prefix) && queryName.length() > prefix.length()) {
                return queryName.substring(prefix.length());
            }
        }
        return queryName;
    }

    void generateAll(String name) throws IOException {
        String entityName = capitalize(name);

        System.out.println("📦 Generating feature: " + feature);
        System.out.println("   Base package: " + featurePackage());
        System.out.println();

        // Domain layer
        generateEntity(entityName);

        // API layer - DTOs
        generateDto("Create" + entityName);
        generateDto("Update" + entityName);
        generateDto(entityName);

        // API layer - Query DTOs
        generateQuery("Get" + entityName);
        generateQuery("List" + entityName);

        // API layer - Controller (after DTOs and Queries are generated)
        generateController(entityName);

        // Application layer - Command handlers
        generateHandler("Create" + entityName);
        generateHandler("Update" + entityName);
        generateHandler("Delete" + entityName);

        // Application layer - Query handlers
        generateQueryHandler("Get" + entityName);
        generateListQueryHandler(entityName);

        // Infrastructure layer
        generateRepository(entityName);

        System.out.println();
        System.out.println("✅ Generated all components for feature: " + feature);
        System.out.println();
        System.out.println("Structure:");
        System.out.println("  " + featurePackage() + "/");
        System.out.println("  ├── api/           # Controllers, DTOs, Queries");
        System.out.println("  ├── domain/        # Entities");
        System.out.println("  ├── application/   # Command & Query Handlers");
        System.out.println("  └── infrastructure/ # Repositories");
    }

    private void writeFile(String subPackage, String className, String content) throws IOException {
        String packagePath = featurePackage().replace('.', '/') + "/" + subPackage;
        Path dir = Paths.get(outputDir, packagePath);
        Files.createDirectories(dir);

        Path file = dir.resolve(className + ".java");
        Files.writeString(file, content);
        System.out.println("✅ Created: " + file);
    }

    private String ensureSuffix(String name, String suffix) {
        return name.endsWith(suffix) ? name : name + suffix;
    }

    private String removeSuffix(String name, String suffix) {
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
