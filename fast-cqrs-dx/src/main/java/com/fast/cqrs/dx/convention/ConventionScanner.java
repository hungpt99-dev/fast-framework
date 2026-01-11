package com.fast.cqrs.dx.convention;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Scans packages using convention-based discovery.
 */
@Component
public class ConventionScanner {

    private static final Logger log = LoggerFactory.getLogger(ConventionScanner.class);

    /**
     * Scans a base package for conventional component locations.
     */
    public void scan(String basePackage) {
        log.debug("Scanning {} for conventional components", basePackage);

        // Convention-based locations
        String controllerPackage = basePackage + ".controller";
        String handlerPackage = basePackage + ".handler";
        String repositoryPackage = basePackage + ".repository";
        String entityPackage = basePackage + ".entity";
        String eventPackage = basePackage + ".event";
        String aggregatePackage = basePackage + ".aggregate";

        log.debug("Expected locations:");
        log.debug("  Controllers: {}", controllerPackage);
        log.debug("  Handlers: {}", handlerPackage);
        log.debug("  Repositories: {}", repositoryPackage);
        log.debug("  Entities: {}", entityPackage);
        log.debug("  Events: {}", eventPackage);
        log.debug("  Aggregates: {}", aggregatePackage);
    }
}
