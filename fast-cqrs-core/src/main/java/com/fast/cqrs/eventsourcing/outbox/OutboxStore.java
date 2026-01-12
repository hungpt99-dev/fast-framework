package com.fast.cqrs.eventsourcing.outbox;

import java.util.List;

/**
 * Store for outbox entries.
 */
public interface OutboxStore {

    /**
     * Saves an outbox entry.
     */
    void save(OutboxEntry entry);

    /**
     * Gets pending entries for publishing.
     */
    List<OutboxEntry> getPending(int limit);

    /**
     * Updates an entry (after publish attempt).
     */
    void update(OutboxEntry entry);

    /**
     * Deletes sent entries older than specified age (cleanup).
     */
    void deleteSentOlderThan(java.time.Duration age);

    /**
     * Marks stale entries as pending (for retry).
     */
    void resetStale(java.time.Duration staleThreshold);
}
