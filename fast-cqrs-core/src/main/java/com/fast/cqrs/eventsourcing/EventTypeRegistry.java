package com.fast.cqrs.eventsourcing;

import com.fast.cqrs.event.DomainEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for mapping event type names to classes.
 * <p>
 * Used by event stores to deserialize events.
 */
public class EventTypeRegistry {

    private final Map<String, Class<? extends DomainEvent>> eventTypes = new ConcurrentHashMap<>();

    /**
     * Registers an event type.
     */
    public void register(Class<? extends DomainEvent> eventClass) {
        eventTypes.put(eventClass.getName(), eventClass);
        eventTypes.put(eventClass.getSimpleName(), eventClass);
    }

    /**
     * Gets the event class for a type name.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends DomainEvent> getEventClass(String typeName) {
        Class<? extends DomainEvent> eventClass = eventTypes.get(typeName);
        
        if (eventClass == null) {
            // Try to load by class name
            try {
                eventClass = (Class<? extends DomainEvent>) Class.forName(typeName);
                eventTypes.put(typeName, eventClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unknown event type: " + typeName, e);
            }
        }
        
        return eventClass;
    }
}
