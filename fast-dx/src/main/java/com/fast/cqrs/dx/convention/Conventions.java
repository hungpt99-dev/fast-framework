package com.fast.cqrs.dx.convention;

/**
 * Defines naming conventions for the framework.
 * <p>
 * Supports feature-based (Clean Architecture) structure:
 * 
 * <pre>
 * {feature}/
 * ├── api/           # Controllers, DTOs
 * ├── domain/        # Entities, Aggregates, Events
 * ├── application/   # Handlers
 * └── infrastructure/ # Repositories
 * </pre>
 */
public final class Conventions {

    private Conventions() {
    }

    // Feature-based package conventions (Clean Architecture)
    public static final String API_PACKAGE = "api";
    public static final String DOMAIN_PACKAGE = "domain";
    public static final String APPLICATION_PACKAGE = "application";
    public static final String INFRASTRUCTURE_PACKAGE = "infrastructure";

    // Legacy layer-based package conventions
    public static final String CONTROLLER_PACKAGE = "controller";
    public static final String HANDLER_PACKAGE = "handler";
    public static final String REPOSITORY_PACKAGE = "repository";
    public static final String ENTITY_PACKAGE = "entity";
    public static final String EVENT_PACKAGE = "event";
    public static final String AGGREGATE_PACKAGE = "aggregate";
    public static final String DTO_PACKAGE = "dto";

    // Suffix conventions
    public static final String CONTROLLER_SUFFIX = "Controller";
    public static final String HANDLER_SUFFIX = "Handler";
    public static final String REPOSITORY_SUFFIX = "Repository";
    public static final String EVENT_SUFFIX = "Event";
    public static final String AGGREGATE_SUFFIX = "Aggregate";
    public static final String COMMAND_SUFFIX = "Cmd";
    public static final String QUERY_SUFFIX = "Query";

    /**
     * Validates controller naming convention.
     * Supports both feature-based (api) and layer-based (controller) packages.
     */
    public static boolean isValidController(String className, String packageName) {
        boolean hasCorrectSuffix = className.endsWith(CONTROLLER_SUFFIX);
        boolean isInApiPackage = packageName.contains("." + API_PACKAGE);
        boolean isInControllerPackage = packageName.contains("." + CONTROLLER_PACKAGE);
        return hasCorrectSuffix && (isInApiPackage || isInControllerPackage);
    }

    /**
     * Validates handler naming convention.
     * Supports both feature-based (application) and layer-based (handler) packages.
     */
    public static boolean isValidHandler(String className, String packageName) {
        boolean hasCorrectSuffix = className.endsWith(HANDLER_SUFFIX);
        boolean isInApplicationPackage = packageName.contains("." + APPLICATION_PACKAGE);
        boolean isInHandlerPackage = packageName.contains("." + HANDLER_PACKAGE);
        return hasCorrectSuffix && (isInApplicationPackage || isInHandlerPackage);
    }

    /**
     * Validates repository naming convention.
     * Supports both feature-based (infrastructure) and layer-based (repository)
     * packages.
     */
    public static boolean isValidRepository(String className, String packageName) {
        boolean hasCorrectSuffix = className.endsWith(REPOSITORY_SUFFIX);
        boolean isInInfrastructurePackage = packageName.contains("." + INFRASTRUCTURE_PACKAGE);
        boolean isInRepositoryPackage = packageName.contains("." + REPOSITORY_PACKAGE);
        return hasCorrectSuffix && (isInInfrastructurePackage || isInRepositoryPackage);
    }

    /**
     * Validates event naming convention.
     */
    public static boolean isValidEvent(String className, String packageName) {
        boolean hasCorrectSuffix = className.endsWith(EVENT_SUFFIX);
        boolean isInDomainPackage = packageName.contains("." + DOMAIN_PACKAGE);
        boolean isInEventPackage = packageName.contains("." + EVENT_PACKAGE);
        return hasCorrectSuffix && (isInDomainPackage || isInEventPackage);
    }

    /**
     * Validates aggregate naming convention.
     */
    public static boolean isValidAggregate(String className, String packageName) {
        boolean hasCorrectSuffix = className.endsWith(AGGREGATE_SUFFIX);
        boolean isInDomainPackage = packageName.contains("." + DOMAIN_PACKAGE);
        boolean isInAggregatePackage = packageName.contains("." + AGGREGATE_PACKAGE);
        return hasCorrectSuffix && (isInDomainPackage || isInAggregatePackage);
    }

    /**
     * Gets expected package for a component type (feature-based).
     */
    public static String getExpectedPackage(String basePackage, String feature, String layer) {
        return basePackage + "." + feature + "." + layer;
    }

    /**
     * Gets expected package for a component type (layer-based, legacy).
     */
    public static String getExpectedPackage(String basePackage, String componentType) {
        return basePackage + "." + componentType;
    }
}
