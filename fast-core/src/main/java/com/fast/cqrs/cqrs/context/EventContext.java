package com.fast.cqrs.cqrs.context;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Context object passed through the event lifecycle.
 */
public class EventContext {
    
    private final String eventId;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    private boolean skipHandler;
    
    public EventContext() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
    }
    
    public String eventId() {
        return eventId;
    }
    
    public Instant timestamp() {
        return timestamp;
    }
    
    public void set(String key, Object value) {
        metadata.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) metadata.get(key);
    }
    
    public void skipHandler() {
        this.skipHandler = true;
    }
    
    public boolean shouldSkipHandler() {
        return skipHandler;
    }
}
