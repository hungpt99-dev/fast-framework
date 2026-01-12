package com.fast.cqrs.eventsourcing.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * JDBC-based snapshot store.
 * <p>
 * Required table:
 * 
 * <pre>{@code
 * CREATE TABLE aggregate_snapshots (
 *     aggregate_id VARCHAR(255) PRIMARY KEY,
 *     aggregate_type VARCHAR(255) NOT NULL,
 *     version BIGINT NOT NULL,
 *     state_data TEXT NOT NULL,
 *     state_type VARCHAR(255) NOT NULL,
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 * );
 * }</pre>
 */
public class JdbcSnapshotStore implements SnapshotStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcSnapshotStore(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, new ObjectMapper());
    }

    public JdbcSnapshotStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> void save(Snapshot<T> snapshot) {
        String sql = """
                INSERT INTO aggregate_snapshots
                (aggregate_id, aggregate_type, version, state_data, state_type, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                version = VALUES(version),
                state_data = VALUES(state_data),
                created_at = VALUES(created_at)
                """;

        try {
            String stateData = objectMapper.writeValueAsString(snapshot.state());
            jdbcTemplate.update(sql,
                    snapshot.aggregateId(),
                    snapshot.aggregateType(),
                    snapshot.version(),
                    stateData,
                    snapshot.state().getClass().getName(),
                    Timestamp.from(snapshot.createdAt()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save snapshot", e);
        }
    }

    @Override
    public <T> Optional<Snapshot<T>> load(String aggregateId, Class<T> stateType) {
        String sql = """
                SELECT aggregate_type, version, state_data, created_at
                FROM aggregate_snapshots
                WHERE aggregate_id = ?
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                try {
                    String stateData = rs.getString("state_data");
                    T state = objectMapper.readValue(stateData, stateType);
                    return Optional.of(new Snapshot<>(
                            aggregateId,
                            rs.getString("aggregate_type"),
                            rs.getLong("version"),
                            state,
                            rs.getTimestamp("created_at").toInstant()));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize snapshot", e);
                }
            }
            return Optional.empty();
        }, aggregateId);
    }

    @Override
    public <T> Optional<Snapshot<T>> loadAt(String aggregateId, long version, Class<T> stateType) {
        String sql = """
                SELECT aggregate_type, state_data, created_at
                FROM aggregate_snapshots
                WHERE aggregate_id = ? AND version = ?
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                try {
                    String stateData = rs.getString("state_data");
                    T state = objectMapper.readValue(stateData, stateType);
                    return Optional.of(new Snapshot<>(
                            aggregateId,
                            rs.getString("aggregate_type"),
                            version,
                            state,
                            rs.getTimestamp("created_at").toInstant()));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize snapshot", e);
                }
            }
            return Optional.empty();
        }, aggregateId, version);
    }

    @Override
    public void delete(String aggregateId) {
        jdbcTemplate.update("DELETE FROM aggregate_snapshots WHERE aggregate_id = ?", aggregateId);
    }

    @Override
    public void deleteOlderThan(String aggregateId, long version) {
        jdbcTemplate.update(
                "DELETE FROM aggregate_snapshots WHERE aggregate_id = ? AND version < ?",
                aggregateId, version);
    }

    /**
     * Creates the snapshot table if it doesn't exist.
     */
    public void createTableIfNotExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS aggregate_snapshots (
                    aggregate_id VARCHAR(255) PRIMARY KEY,
                    aggregate_type VARCHAR(255) NOT NULL,
                    version BIGINT NOT NULL,
                    state_data TEXT NOT NULL,
                    state_type VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        jdbcTemplate.execute(sql);
    }
}
