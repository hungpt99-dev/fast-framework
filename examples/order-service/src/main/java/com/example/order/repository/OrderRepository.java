package com.example.order.repository;

import com.example.order.entity.Order;
import com.fast.cqrs.sql.annotation.Param;
import com.fast.cqrs.sql.annotation.Select;
import com.fast.cqrs.sql.annotation.SqlRepository;
import com.fast.cqrs.sql.repository.FastRepository;

import java.util.List;

/**
 * SQL Repository for Order operations with performance features.
 * 
 * Extends FastRepository for automatic CRUD operations.
 * Uses @Select with cache and metrics options.
 */
@SqlRepository
public interface OrderRepository extends FastRepository<Order, String> {

    // CRUD methods from FastRepository:
    // - findById(String id)
    // - findAll()
    // - save(Order entity)
    // - saveAll(List<Order> entities)
    // - updateAll(List<Order> entities)
    // - deleteById(String id)
    // - deleteAllById(List<String> ids)
    // - existsById(String id)
    // - count()
    // - deleteAll()

    /**
     * Find orders by customer with 1 minute cache.
     */
    @Select(value = "SELECT * FROM orders WHERE customer_id = :customerId",
            cache = "1m")
    List<Order> findByCustomerId(@Param("customerId") String customerId);

    /**
     * Find orders by status with metrics enabled.
     */
    @Select(value = "SELECT * FROM orders WHERE status = :status",
            metrics = true)
    List<Order> findByStatus(@Param("status") String status);

    /**
     * Complex query with cache, timeout, and metrics.
     */
    @Select(value = "SELECT * FROM orders WHERE customer_id = :customerId AND status = :status",
            cache = "30s",
            timeout = "5s",
            metrics = true,
            metricsName = "orders.findByCustomerAndStatus")
    List<Order> findByCustomerIdAndStatus(
            @Param("customerId") String customerId,
            @Param("status") String status);
}
