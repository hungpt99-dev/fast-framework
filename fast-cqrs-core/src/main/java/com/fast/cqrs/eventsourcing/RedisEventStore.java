package com.fast.cqrs.eventsourcing;

import com.fast.cqrs.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Redis-based event store implementation.
 * <p>
 * Stores events in Redis using lists per aggregate.
 * <p>
 * Requires Spring Data Redis on the classpath.
 * <p>
 * Key structure:
 * <ul>
 *   <li>{@code events:{aggregateId}} - List of serialized events</li>
 *   <li>{@code version:{aggregateId}} - Current version number</li>
 * </ul>
 */
public class RedisEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(RedisEventStore.class);

    private static final String EVENTS_PREFIX = "events:";
    private static final String VERSION_PREFIX = "version:";

    private final RedisOperations redis;
    private final ObjectMapper objectMapper;
    private final EventTypeRegistry eventTypeRegistry;

    public RedisEventStore(RedisOperations redis, ObjectMapper objectMapper,
                           EventTypeRegistry eventTypeRegistry) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.eventTypeRegistry = eventTypeRegistry;
    }

    public RedisEventStore(RedisOperations redis) {
        this(redis, new ObjectMapper(), new EventTypeRegistry());
    }

    @Override
    public void append(String aggregateId, List<DomainEvent> events, long expectedVersion) {
        String eventsKey = EVENTS_PREFIX + aggregateId;
        String versionKey = VERSION_PREFIX + aggregateId;

        long currentVersion = getVersion(aggregateId).orElse(-1L);

        if (expectedVersion != -1 && currentVersion != expectedVersion) {
            throw new InMemoryEventStore.ConcurrencyException(
                "Concurrency conflict for aggregate " + aggregateId +
                ": expected version " + expectedVersion + ", but was " + currentVersion
            );
        }

        for (DomainEvent event : events) {
            try {
                EventEnvelope envelope = new EventEnvelope(
                    event.getClass().getName(),
                    objectMapper.writeValueAsString(event)
                );
                redis.rightPush(eventsKey, objectMapper.writeValueAsString(envelope));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize event", e);
            }
        }

        redis.set(versionKey, String.valueOf(currentVersion + events.size()));
        log.debug("Appended {} events to aggregate {}", events.size(), aggregateId);
    }

    @Override
    public List<DomainEvent> load(String aggregateId) {
        String eventsKey = EVENTS_PREFIX + aggregateId;
        List<String> serialized = redis.range(eventsKey, 0, -1);
        return deserializeEvents(serialized);
    }

    @Override
    public List<DomainEvent> loadFrom(String aggregateId, long fromVersion) {
        String eventsKey = EVENTS_PREFIX + aggregateId;
        List<String> serialized = redis.range(eventsKey, fromVersion + 1, -1);
        return deserializeEvents(serialized);
    }

    @Override
    public Optional<Long> getVersion(String aggregateId) {
        String versionKey = VERSION_PREFIX + aggregateId;
        String version = redis.get(versionKey);
        return version != null ? Optional.of(Long.parseLong(version)) : Optional.empty();
    }

    private List<DomainEvent> deserializeEvents(List<String> serialized) {
        List<DomainEvent> events = new ArrayList<>();
        for (String json : serialized) {
            try {
                EventEnvelope envelope = objectMapper.readValue(json, EventEnvelope.class);
                Class<? extends DomainEvent> eventClass = eventTypeRegistry.getEventClass(envelope.type());
                events.add(objectMapper.readValue(envelope.data(), eventClass));
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize event", e);
            }
        }
        return events;
    }

    /**
     * Event envelope for storing type information.
     */
    public record EventEnvelope(String type, String data) {}

    /**
     * Redis operations interface for abstraction.
     */
    public interface RedisOperations {
        void rightPush(String key, String value);
        List<String> range(String key, long start, long end);
        void set(String key, String value);
        String get(String key);
    }
}
