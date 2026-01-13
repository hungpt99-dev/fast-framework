package com.fast.cqrs.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
 */
@AutoConfiguration
@ConditionalOnWebApplication
@Import(CqrsRegistrarConfiguration.class)
public class CqrsAutoConfiguration {

    // Auto-configuration marker class
    // Actual bean definitions are in CqrsRegistrarConfiguration
    // Controller scanning is triggered by @EnableCqrs
    
}
