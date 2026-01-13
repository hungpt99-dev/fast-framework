package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.sql.autoconfigure.SqlRepositoryImportRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Single annotation to enable all Fast Framework features.
 * <p>
 * Zero configuration - everything is convention-based.
 * <p>
 * Example:
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;SpringBootApplication
 *     @EnableFast
 *     public class Application {
 *         public static void main(String[] args) {
 *             SpringApplication.run(Application.class, args);
 *         }
 *     }
 * }
 * </pre>
 * 
 * <h2>Conventions</h2>
 * <ul>
 * <li>Controllers: {@code *.controller} package, suffix {@code Controller}</li>
 * <li>Handlers: {@code *.handler} package, suffix {@code Handler}</li>
 * <li>Repositories: {@code *.repository} package, suffix
 * {@code Repository}</li>
 * <li>Entities: {@code *.entity} package</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        FastAutoConfiguration.class,
        CqrsRegistrarConfiguration.class,
        HttpControllerRegistrar.class,
        SqlRepositoryImportRegistrar.class
})
public @interface EnableFast {

    /**
     * Base packages to scan. Defaults to the package of the annotated class.
     */
    String[] basePackages() default {};

    /**
     * Enable strict naming convention enforcement.
     */
    boolean strict() default false;
}
