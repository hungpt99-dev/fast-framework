package com.fast.cqrs.dx.convention;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit-based architecture rules for Fast Framework conventions.
 * <p>
 * Supports both layer-based and feature-based (Clean Architecture) structures.
 * <p>
 * <h2>Feature-Based Structure (Recommended)</h2>
 * 
 * <pre>
 * com.example.order/
 * ├── api/           # Controllers, DTOs
 * ├── domain/        # Entities, Aggregates, Events
 * ├── application/   # Handlers
 * └── infrastructure/ # Repositories
 * </pre>
 * <p>
 * Example usage in tests:
 * 
 * <pre>
 * {
 *         &#64;code
 *         &#64;AnalyzeClasses(packages = "com.example")
 *         public class ArchitectureTest {
 *                 @ArchTest
 *                 ArchRule controllerRule = FastConventionRules.CONTROLLERS_IN_API_PACKAGE;
 *         }
 * }
 * </pre>
 */
public final class FastConventionRules {

        private FastConventionRules() {
        }

        // ==================== Feature-Based Rules (Clean Architecture)
        // ====================

        /**
         * Controllers should be in a *.api package.
         */
        public static final ArchRule CONTROLLERS_IN_API_PACKAGE = classes()
                        .that().haveSimpleNameEndingWith("Controller")
                        .should().resideInAPackage("..api..")
                        .as("Controllers should reside in '*.api' package");

        /**
         * Handlers should be in a *.application package.
         */
        public static final ArchRule HANDLERS_IN_APPLICATION_PACKAGE = classes()
                        .that().haveSimpleNameEndingWith("Handler")
                        .should().resideInAPackage("..application..")
                        .as("Handlers should reside in '*.application' package");

        /**
         * Repositories should be in a *.infrastructure package.
         */
        public static final ArchRule REPOSITORIES_IN_INFRASTRUCTURE_PACKAGE = classes()
                        .that().haveSimpleNameEndingWith("Repository")
                        .should().resideInAPackage("..infrastructure..")
                        .as("Repositories should reside in '*.infrastructure' package");

        /**
         * Entities should be in a *.domain package.
         */
        public static final ArchRule ENTITIES_IN_DOMAIN_PACKAGE = classes()
                        .that().areAnnotatedWith("com.fast.cqrs.sql.repository.Table")
                        .should().resideInAPackage("..domain..")
                        .as("Entities should reside in '*.domain' package");

        /**
         * Aggregates should be in a *.domain package.
         */
        public static final ArchRule AGGREGATES_IN_DOMAIN_PACKAGE = classes()
                        .that().haveSimpleNameEndingWith("Aggregate")
                        .should().resideInAPackage("..domain..")
                        .as("Aggregates should reside in '*.domain' package");

        /**
         * Events should be in a *.domain package.
         */
        public static final ArchRule EVENTS_IN_DOMAIN_PACKAGE = classes()
                        .that().haveSimpleNameEndingWith("Event")
                        .should().resideInAPackage("..domain..")
                        .as("Events should reside in '*.domain' package");

        /**
         * DTOs (Cmd, Query, Dto) should be in a *.api package.
         */
        public static final ArchRule DTOS_IN_API_PACKAGE = classes()
                        .that().haveSimpleNameEndingWith("Cmd")
                        .or().haveSimpleNameEndingWith("Dto")
                        .should().resideInAPackage("..api..")
                        .as("DTOs should reside in '*.api' package");

        // ==================== Clean Architecture Dependency Rules ====================

        /**
         * Domain layer should not depend on application layer.
         */
        public static final ArchRule DOMAIN_SHOULD_NOT_DEPEND_ON_APPLICATION = noClasses()
                        .that().resideInAPackage("..domain..")
                        .should().dependOnClassesThat().resideInAPackage("..application..")
                        .as("Domain should not depend on Application layer");

        /**
         * Domain layer should not depend on infrastructure layer.
         */
        public static final ArchRule DOMAIN_SHOULD_NOT_DEPEND_ON_INFRASTRUCTURE = noClasses()
                        .that().resideInAPackage("..domain..")
                        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                        .as("Domain should not depend on Infrastructure layer");

        /**
         * Domain layer should not depend on API layer.
         */
        public static final ArchRule DOMAIN_SHOULD_NOT_DEPEND_ON_API = noClasses()
                        .that().resideInAPackage("..domain..")
                        .should().dependOnClassesThat().resideInAPackage("..api..")
                        .as("Domain should not depend on API layer");

        /**
         * Application layer should not depend on infrastructure layer.
         */
        public static final ArchRule APPLICATION_SHOULD_NOT_DEPEND_ON_INFRASTRUCTURE = noClasses()
                        .that().resideInAPackage("..application..")
                        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                        .as("Application should not depend on Infrastructure layer");

        // ==================== Legacy Layer-Based Rules ====================

        /**
         * Controllers in controller package (legacy layer-based).
         */
        public static final ArchRule CONTROLLERS_IN_CONTROLLER_PACKAGE = classes()
                        .that().haveSimpleNameEndingWith("Controller")
                        .should().resideInAPackage("..controller..")
                        .orShould().resideInAPackage("..api..")
                        .as("Controllers should reside in '*.controller' or '*.api' package");

        /**
         * Handlers in handler package (legacy layer-based).
         */
        public static final ArchRule HANDLERS_IN_HANDLER_PACKAGE = classes()
                        .that().haveSimpleNameEndingWith("Handler")
                        .should().resideInAPackage("..handler..")
                        .orShould().resideInAPackage("..application..")
                        .as("Handlers should reside in '*.handler' or '*.application' package");

        /**
         * Repositories in repository package (legacy layer-based).
         */
        public static final ArchRule REPOSITORIES_IN_REPOSITORY_PACKAGE = classes()
                        .that().haveSimpleNameEndingWith("Repository")
                        .should().resideInAPackage("..repository..")
                        .orShould().resideInAPackage("..infrastructure..")
                        .as("Repositories should reside in '*.repository' or '*.infrastructure' package");

        // ==================== Rule Sets ====================

        /**
         * Feature-based (Clean Architecture) rules.
         */
        public static final ArchRule[] FEATURE_BASED_RULES = {
                        CONTROLLERS_IN_API_PACKAGE,
                        HANDLERS_IN_APPLICATION_PACKAGE,
                        REPOSITORIES_IN_INFRASTRUCTURE_PACKAGE,
                        AGGREGATES_IN_DOMAIN_PACKAGE,
                        EVENTS_IN_DOMAIN_PACKAGE,
                        DTOS_IN_API_PACKAGE,
                        DOMAIN_SHOULD_NOT_DEPEND_ON_APPLICATION,
                        DOMAIN_SHOULD_NOT_DEPEND_ON_INFRASTRUCTURE,
                        DOMAIN_SHOULD_NOT_DEPEND_ON_API,
                        APPLICATION_SHOULD_NOT_DEPEND_ON_INFRASTRUCTURE
        };

        /**
         * Legacy layer-based rules (supports both structures).
         */
        public static final ArchRule[] LAYER_BASED_RULES = {
                        CONTROLLERS_IN_CONTROLLER_PACKAGE,
                        HANDLERS_IN_HANDLER_PACKAGE,
                        REPOSITORIES_IN_REPOSITORY_PACKAGE
        };

        /**
         * All rules for convenience.
         */
        public static final ArchRule[] ALL_RULES = FEATURE_BASED_RULES;
}
