package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.dx.convention.ConventionScanner;
import com.fast.cqrs.dx.convention.NamingConventionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

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
 * <p>
 * <b>Configuration Options:</b>
 * <pre>{@code
 * fast:
 *   native:
 *     enabled: true   # Enable GraalVM native image support (auto-detected by default)
 *   aot:
 *     enabled: true   # Use AOT-generated code (recommended for native)
 * }</pre>
 */
@Configuration
@EnableConfigurationProperties(FastProperties.class)
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

    /**
     * Nested configuration for GraalVM native image support.
     * Only activated when fast.native.enabled=true (or auto-detected).
     */
    @Configuration
    @ConditionalOnProperty(name = "fast.native.enabled", havingValue = "true", matchIfMissing = true)
    @ImportRuntimeHints(FastFrameworkRuntimeHints.class)
    static class NativeImageConfiguration {
        
        NativeImageConfiguration() {
            LoggerFactory.getLogger(NativeImageConfiguration.class)
                    .debug("Fast Framework GraalVM native image support enabled");
        }
    }
}
