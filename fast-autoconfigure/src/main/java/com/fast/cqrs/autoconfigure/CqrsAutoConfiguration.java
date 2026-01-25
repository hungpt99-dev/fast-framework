package com.fast.cqrs.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot auto-configuration for the CQRS framework.
 * <p>
 * This configuration is automatically enabled in Spring Boot applications
 * that include the {@code fast-cqrs-autoconfigure} dependency.
 * <p>
 * To use the framework, add {@link EnableCqrs} to your main application class
 * to specify which packages should be scanned for {@code @HttpController} interfaces.
 * <p>
 * <b>GraalVM Native Image:</b> Configure via properties:
 * <pre>{@code
 * fast:
 *   native:
 *     enabled: true   # Enable/disable native image support (default: auto-detect)
 * }</pre>
 * For optimal native image performance, use the annotation processor (fast-processor)
 * which generates zero-reflection implementations at compile time.
 *
 * @see FastAutoConfiguration.NativeImageConfiguration
 */
@AutoConfiguration
@ConditionalOnWebApplication
@Import({CqrsRegistrarConfiguration.class, FastAutoConfiguration.class})
public class CqrsAutoConfiguration {

    // Auto-configuration marker class
    // Actual bean definitions are in CqrsRegistrarConfiguration
    // Controller scanning is triggered by @EnableCqrs
    // Native image support is conditionally enabled via FastAutoConfiguration.NativeImageConfiguration
    
}
