package com.fast.cqrs.cqrs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables caching for a query method.
 * <p>
 * Results will be cached using the specified key and TTL.
 * <p>
 * Example:
 * <pre>{@code
 * @CacheableQuery(key = "#query.id", ttl = "5m")
 * @Query
 * OrderDto getOrder(@RequestBody GetOrderQuery query);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheableQuery {
    
    /**
     * Cache key expression using SpEL.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code #query.id} - Use the id property of the query parameter</li>
     *   <li>{@code #p0} - Use the first parameter</li>
     *   <li>{@code 'static-key'} - Use a static key</li>
     * </ul>
     */
    String key() default "";
    
    /**
     * Time-to-live for cached entries.
     * <p>
     * Format: number + unit (s/m/h/d)
     * Examples: "30s", "5m", "1h", "1d"
     * <p>
     * Default: "5m" (5 minutes)
     */
    String ttl() default "5m";
    
    /**
     * Cache name/region.
     * Default: method name
     */
    String cacheName() default "";
}
