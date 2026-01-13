package com.fast.cqrs.sql.repository;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface providing standard CRUD operations.
 * <p>
 * Extend this interface in your {@code @SqlRepository} to get automatic
 * CRUD method implementations without writing SQL.
 * <p>
 * Example:
 * <pre>{@code
 * @SqlRepository
 * public interface OrderRepository extends FastRepository<Order, Long> {
 *     // CRUD methods available automatically!
 *     
 *     // Add custom queries as needed:
 *     @Select("SELECT * FROM orders WHERE customer_id = :customerId")
 *     List<Order> findByCustomerId(@Param("customerId") Long customerId);
 * }
 * }</pre>
 *
 * @param <T>  the entity type
 * @param <ID> the ID type
 */
public interface FastRepository<T, ID> {

    /**
     * Finds an entity by its ID.
     *
     * @param id the entity ID
     * @return an Optional containing the entity, or empty if not found
     */
    Optional<T> findById(ID id);

    /**
     * Returns all entities.
     *
     * @return list of all entities
     */
    List<T> findAll();

    /**
     * Saves an entity (insert or update).
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    T save(T entity);

    /**
     * Saves multiple entities (batch insert).
     *
     * @param entities the entities to save
     */
    void saveAll(List<T> entities);

    /**
     * Updates multiple entities (batch update).
     *
     * @param entities the entities to update
     */
    void updateAll(List<T> entities);

    /**
     * Deletes an entity by its ID.
     *
     * @param id the entity ID
     */
    void deleteById(ID id);

    /**
     * Deletes multiple entities by their IDs (batch delete).
     *
     * @param ids the entity IDs
     */
    void deleteAllById(List<ID> ids);

    /**
     * Checks if an entity exists by ID.
     *
     * @param id the entity ID
     * @return true if exists
     */
    boolean existsById(ID id);

    /**
     * Returns the count of all entities.
     *
     * @return total count
     */
    long count();

    /**
     * Deletes all entities.
     */
    void deleteAll();
}
