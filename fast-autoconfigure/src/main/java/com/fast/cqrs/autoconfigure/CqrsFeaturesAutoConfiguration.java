package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.cqrs.cache.InMemoryQueryCache;
import com.fast.cqrs.cqrs.cache.QueryCache;
import com.fast.cqrs.cqrs.idempotency.IdempotencyStore;
import com.fast.cqrs.cqrs.idempotency.InMemoryIdempotencyStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for CQRS caching and idempotency features.
 * <p>
 * Beans are created only when the features are enabled (default: enabled).
 * <p>
 * Configuration properties:
 * <pre>{@code
 * fast:
 *   cqrs:
 *     cache:
 *       enabled: true  # Enable query caching (default: true)
 *     idempotency:
 *       enabled: true  # Enable command idempotency (default: true)
 * }</pre>
 */
@Configuration
@EnableAspectJAutoProxy
public class CqrsFeaturesAutoConfiguration {

    // ==================== Query Caching ====================

    @Bean
    @ConditionalOnMissingBean(QueryCache.class)
    @ConditionalOnProperty(name = "fast.cqrs.cache.enabled", havingValue = "true", matchIfMissing = true)
    public QueryCache queryCache() {
        return new InMemoryQueryCache();
    }

    @Bean
    @ConditionalOnMissingBean(QueryCacheAspect.class)
    @ConditionalOnProperty(name = "fast.cqrs.cache.enabled", havingValue = "true", matchIfMissing = true)
    public QueryCacheAspect queryCacheAspect(QueryCache queryCache) {
        return new QueryCacheAspect(queryCache);
    }

    // ==================== Command Idempotency ====================

    @Bean
    @ConditionalOnMissingBean(IdempotencyStore.class)
    @ConditionalOnProperty(name = "fast.cqrs.idempotency.enabled", havingValue = "true", matchIfMissing = true)
    public IdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyAspect.class)
    @ConditionalOnProperty(name = "fast.cqrs.idempotency.enabled", havingValue = "true", matchIfMissing = true)
    public IdempotencyAspect idempotencyAspect(IdempotencyStore idempotencyStore) {
        return new IdempotencyAspect(idempotencyStore);
    }
}
