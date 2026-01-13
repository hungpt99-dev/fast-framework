package com.fast.cqrs.sql.repository;

import java.util.List;

/**
 * Extension of {@link FastRepository} with pagination and sorting support.
 * <p>
 * Example:
 * <pre>{@code
 * @SqlRepository
 * public interface OrderRepository extends PagingRepository<Order, Long> {
 *     // All CRUD + paging methods available!
 * }
 * }</pre>
 *
 * @param <T>  the entity type
 * @param <ID> the ID type
 */
public interface PagingRepository<T, ID> extends FastRepository<T, ID> {

    /**
     * Returns a page of entities.
     *
     * @param pageable pagination information
     * @return a page of entities
     */
    Page<T> findAll(Pageable pageable);

    /**
     * Returns all entities sorted.
     *
     * @param sort sorting information
     * @return sorted list of entities
     */
    List<T> findAll(Sort sort);
}
