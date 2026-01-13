package com.fast.cqrs.cqrs.interceptor;

import com.fast.cqrs.cqrs.annotation.CacheableQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Interceptor that caches query results based on {@link CacheableQuery} annotation.
 */
public class CacheInterceptor implements CommandInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CacheInterceptor.class);
    
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Object intercept(InterceptorContext context, InterceptorChain chain) throws Exception {
        Method method = context.getMethod();
        CacheableQuery cacheableQuery = method.getAnnotation(CacheableQuery.class);
        
        if (cacheableQuery == null) {
            return chain.proceed(context);
        }

        String cacheKey = buildCacheKey(method, context.getArgs(), cacheableQuery);
        long ttlMs = parseTtl(cacheableQuery.ttl());

        // Check cache
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            log.debug("Cache hit for key: {}", cacheKey);
            return entry.value;
        }

        // Execute and cache
        log.debug("Cache miss for key: {}", cacheKey);
        Object result = chain.proceed(context);
        cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis() + ttlMs));
        
        return result;
    }

    private String buildCacheKey(Method method, Object[] args, CacheableQuery annotation) {
        StringBuilder key = new StringBuilder();
        
        String cacheName = annotation.cacheName().isEmpty() 
            ? method.getDeclaringClass().getSimpleName() + "." + method.getName()
            : annotation.cacheName();
        key.append(cacheName);
        
        if (args != null) {
            for (Object arg : args) {
                key.append(":").append(arg != null ? arg.hashCode() : "null");
            }
        }
        
        return key.toString();
    }

    private long parseTtl(String ttl) {
        if (ttl.endsWith("ms")) {
            return Long.parseLong(ttl.replace("ms", ""));
        } else if (ttl.endsWith("s")) {
            return TimeUnit.SECONDS.toMillis(Long.parseLong(ttl.replace("s", "")));
        } else if (ttl.endsWith("m")) {
            return TimeUnit.MINUTES.toMillis(Long.parseLong(ttl.replace("m", "")));
        } else if (ttl.endsWith("h")) {
            return TimeUnit.HOURS.toMillis(Long.parseLong(ttl.replace("h", "")));
        } else if (ttl.endsWith("d")) {
            return TimeUnit.DAYS.toMillis(Long.parseLong(ttl.replace("d", "")));
        }
        return TimeUnit.MINUTES.toMillis(5); // Default 5 minutes
    }

    @Override
    public int getOrder() {
        return 100; // Run early
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        cache.clear();
    }

    private record CacheEntry(Object value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
