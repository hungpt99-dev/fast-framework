package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.dx.convention.ConventionScanner;
import com.fast.cqrs.dx.convention.NamingConventionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Auto-configuration for Fast Framework.
 * <p>
 * Automatically scans and configures:
 * <ul>
 * <li>CQRS Controllers</li>
 * <li>SQL Repositories</li>
 * <li>Event Handlers</li>
 * <li>Aggregates</li>
 * </ul>
 */
@Configuration
public class FastAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FastAutoConfiguration.class);

    @Bean
    public ConventionScanner conventionScanner() {
        log.info("Fast Framework initialized with convention-based scanning");
        return new ConventionScanner();
    }

    @Bean
    public NamingConventionValidator namingConventionValidator() {
        return new NamingConventionValidator();
    }
}
