package com.fast.cqrs.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Enables the CQRS controller framework.
 * <p>
 * Add this annotation to a {@code @Configuration} class to enable
 * automatic discovery and registration of {@code @HttpController} interfaces.
 * <p>
 * Example:
 * <pre>{@code
 * @SpringBootApplication
 * @EnableCqrs(basePackages = "com.example.controllers")
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * }</pre>
 *
 * @see com.fast.cqrs.annotation.HttpController
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({CqrsRegistrarConfiguration.class, HttpControllerRegistrar.class})
public @interface EnableCqrs {

    /**
     * Base packages to scan for {@code @HttpController} interfaces.
     * <p>
     * If not specified, scanning will start from the package of the
     * class that declares this annotation.
     *
     * @return the base packages to scan
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying
     * the packages to scan for annotated controllers.
     * <p>
     * The package of each class specified will be scanned.
     *
     * @return the base package classes
     */
    Class<?>[] basePackageClasses() default {};
}
