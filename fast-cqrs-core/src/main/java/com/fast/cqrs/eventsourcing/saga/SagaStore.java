package com.fast.cqrs.eventsourcing.saga;

import java.util.Optional;

/**
 * Store for saga instances.
 */
public interface SagaStore {

    /**
     * Saves a saga.
     */
    void save(Saga saga);

    /**
     * Loads a saga by ID.
     */
    <T extends Saga> Optional<T> load(String sagaId, Class<T> sagaType);

    /**
     * Finds a saga by association.
     */
    <T extends Saga> Optional<T> findByAssociation(String associationKey, String associationValue, Class<T> sagaType);

    /**
     * Deletes a saga.
     */
    void delete(String sagaId);
}
