package com.fast.cqrs.sql.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Enables SQL Repository scanning and registration.
 * <p>
 * Add this annotation to a {@code @Configuration} class to enable
 * automatic discovery of {@code @SqlRepository} interfaces.
 * <p>
 * Example:
 * <pre>{@code
 * @SpringBootApplication
 * @EnableSqlRepositories(basePackages = "com.example.repository")
 * public class MyApplication { }
 * }</pre>
 *
 * @see com.fast.cqrs.sql.annotation.SqlRepository
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({SqlRepositoryConfiguration.class, SqlRepositoryImportRegistrar.class})
public @interface EnableSqlRepositories {

    /**
     * Base packages to scan for {@code @SqlRepository} interfaces.
     *
     * @return the base packages
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()}.
     *
     * @return the base package classes
     */
    Class<?>[] basePackageClasses() default {};
}
