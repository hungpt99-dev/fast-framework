package com.fast.cqrs.cqrs.context;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Context object passed through the command lifecycle.
 * <p>
 * Provides access to:
 * <ul>
 *   <li>Command metadata (id, timestamp)</li>
 *   <li>User information and permissions</li>
 *   <li>Shared data between lifecycle phases</li>
 * </ul>
 */
public class CommandContext {
    
    private final String commandId;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    private Object result;
    private boolean skipHandler;
    
    public CommandContext() {
        this.commandId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
    }
    
    public CommandContext(String commandId) {
        this.commandId = commandId;
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
    }
    
    // === Getters ===
    
    public String commandId() {
        return commandId;
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
    
    public <T> T get(String key, Class<T> type, T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? type.cast(value) : defaultValue;
    }
    
    public Map<String, Object> metadata() {
        return Map.copyOf(metadata);
    }
    
    // === Security ===
    
    public String user() {
        return get("user", String.class, "anonymous");
    }
    
    public void setUser(String user) {
        set("user", user);
    }
    
    public boolean hasPermission(String permission) {
        @SuppressWarnings("unchecked")
        var permissions = get("permissions", java.util.Set.class);
        return permissions != null && permissions.contains(permission);
    }
    
    // === Flow Control ===
    
    /**
     * Get the result set by preHandle or postHandle.
     */
    public Object result() {
        return result;
    }
    
    /**
     * Set a result (useful for returning early from preHandle).
     */
    public void setResult(Object result) {
        this.result = result;
    }
    
    /**
     * Skip the main handler execution.
     * Useful when preHandle provides a cached result.
     */
    public void skipHandler() {
        this.skipHandler = true;
    }
    
    public boolean shouldSkipHandler() {
        return skipHandler;
    }
}
