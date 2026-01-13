package com.fast.cqrs.cqrs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets a timeout for method execution.
 * <p>
 * Example:
 * <pre>{@code
 * @Timeout("5s")
 * @Query
 * ReportDto generateReport(@RequestBody GenerateReportQuery query);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timeout {
    
    /**
     * Timeout duration.
     * Format: number + unit (ms/s/m)
     */
    String value() default "30s";
}
