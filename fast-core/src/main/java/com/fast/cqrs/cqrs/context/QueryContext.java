package com.fast.cqrs.cqrs.context;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Context object passed through the query lifecycle.
 * <p>
 * Provides access to:
 * <ul>
 *   <li>Query metadata (id, timestamp)</li>
 *   <li>Cache hints</li>
 *   <li>Shared data between lifecycle phases</li>
 * </ul>
 */
public class QueryContext {
    
    private final String queryId;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    private Object cachedResult;
    private boolean cacheHit;
    
    public QueryContext() {
        this.queryId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
    }
    
    public QueryContext(String queryId) {
        this.queryId = queryId;
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
    }
    
    // === Getters ===
    
    public String queryId() {
        return queryId;
    }
    
    public Instant timestamp() {
        return timestamp;
    }
    
    // === Metadata ===
    
    public void set(String key, Object value) {
        metadata.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) metadata.get(key);
    }
    
    public Map<String, Object> metadata() {
        return Map.copyOf(metadata);
    }
    
    // === User ===
    
    public String user() {
        return get("user", String.class);
    }
    
    public void setUser(String user) {
        set("user", user);
    }
    
    // === Cache ===
    
    /**
     * Mark that a cache hit occurred in preQuery.
     */
    public void setCacheHit(Object cachedResult) {
        this.cachedResult = cachedResult;
        this.cacheHit = true;
    }
    
    public boolean isCacheHit() {
        return cacheHit;
    }
    
    public Object cachedResult() {
        return cachedResult;
    }
}
