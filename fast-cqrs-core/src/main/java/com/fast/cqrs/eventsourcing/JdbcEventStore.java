package com.fast.cqrs.eventsourcing;

import com.fast.cqrs.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based event store implementation.
 * <p>
 * Stores events in a relational database.
 * <p>
 * Required table:
 * <pre>{@code
 * CREATE TABLE event_store (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     aggregate_id VARCHAR(255) NOT NULL,
 *     event_type VARCHAR(255) NOT NULL,
 *     event_data TEXT NOT NULL,
 *     version BIGINT NOT NULL,
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     INDEX idx_aggregate_id (aggregate_id),
 *     UNIQUE KEY uk_aggregate_version (aggregate_id, version)
 * );
 * }</pre>
 */
public class JdbcEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcEventStore.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EventTypeRegistry eventTypeRegistry;

    public JdbcEventStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, 
                          EventTypeRegistry eventTypeRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.eventTypeRegistry = eventTypeRegistry;
    }

    public JdbcEventStore(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, new ObjectMapper(), new EventTypeRegistry());
    }

    @Override
    public void append(String aggregateId, List<DomainEvent> events, long expectedVersion) {
        long currentVersion = getVersion(aggregateId).orElse(-1L);
        
        if (expectedVersion != -1 && currentVersion != expectedVersion) {
            throw new InMemoryEventStore.ConcurrencyException(
                "Concurrency conflict for aggregate " + aggregateId +
                ": expected version " + expectedVersion + ", but was " + currentVersion
            );
        }

        String sql = "INSERT INTO event_store (aggregate_id, event_type, event_data, version, created_at) VALUES (?, ?, ?, ?, ?)";
        
        long version = currentVersion;
        for (DomainEvent event : events) {
            version++;
            try {
                String eventData = objectMapper.writeValueAsString(event);
                jdbcTemplate.update(sql, 
                    aggregateId, 
                    event.getClass().getName(),
                    eventData,
                    version,
                    Timestamp.from(Instant.now())
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize event", e);
            }
        }

        log.debug("Appended {} events to aggregate {}", events.size(), aggregateId);
    }

    @Override
    public List<DomainEvent> load(String aggregateId) {
        String sql = "SELECT event_type, event_data FROM event_store WHERE aggregate_id = ? ORDER BY version";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> deserializeEvent(rs), aggregateId);
    }

    @Override
    public List<DomainEvent> loadFrom(String aggregateId, long fromVersion) {
        String sql = "SELECT event_type, event_data FROM event_store WHERE aggregate_id = ? AND version > ? ORDER BY version";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> deserializeEvent(rs), aggregateId, fromVersion);
    }

    @Override
    public Optional<Long> getVersion(String aggregateId) {
        String sql = "SELECT MAX(version) FROM event_store WHERE aggregate_id = ?";
        Long version = jdbcTemplate.queryForObject(sql, Long.class, aggregateId);
        return Optional.ofNullable(version);
    }

    private DomainEvent deserializeEvent(ResultSet rs) {
        try {
            String eventType = rs.getString("event_type");
            String eventData = rs.getString("event_data");
            
            Class<? extends DomainEvent> eventClass = eventTypeRegistry.getEventClass(eventType);
            return objectMapper.readValue(eventData, eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }

    /**
     * Creates the event store table if it doesn't exist.
     */
    public void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS event_store (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                aggregate_id VARCHAR(255) NOT NULL,
                event_type VARCHAR(255) NOT NULL,
                event_data TEXT NOT NULL,
                version BIGINT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_aggregate_id (aggregate_id)
            )
            """;
        jdbcTemplate.execute(sql);
    }
}
