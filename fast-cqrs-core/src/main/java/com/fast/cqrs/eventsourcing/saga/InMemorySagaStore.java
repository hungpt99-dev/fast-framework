package com.fast.cqrs.eventsourcing.saga;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory saga store for testing.
 */
public class InMemorySagaStore implements SagaStore {

    private final Map<String, Saga> sagas = new ConcurrentHashMap<>();
    private final Map<String, String> associations = new ConcurrentHashMap<>();

    @Override
    public void save(Saga saga) {
        sagas.put(saga.getSagaId(), saga);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Saga> Optional<T> load(String sagaId, Class<T> sagaType) {
        Saga saga = sagas.get(sagaId);
        if (saga != null && sagaType.isInstance(saga)) {
            return Optional.of((T) saga);
        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Saga> Optional<T> findByAssociation(String associationKey, String associationValue,
            Class<T> sagaType) {
        String sagaId = associations.get(associationKey + ":" + associationValue);
        if (sagaId != null) {
            return load(sagaId, sagaType);
        }
        return Optional.empty();
    }

    @Override
    public void delete(String sagaId) {
        sagas.remove(sagaId);
    }

    public void addAssociation(String sagaId, String key, String value) {
        associations.put(key + ":" + value, sagaId);
    }

    public void clear() {
        sagas.clear();
        associations.clear();
    }
}
