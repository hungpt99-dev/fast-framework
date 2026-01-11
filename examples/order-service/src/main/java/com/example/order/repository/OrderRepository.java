package com.example.order.repository;

import com.example.order.entity.Order;
import com.fast.cqrs.sql.annotation.Param;
import com.fast.cqrs.sql.annotation.Select;
import com.fast.cqrs.sql.annotation.SqlRepository;
import com.fast.cqrs.sql.repository.FastRepository;

import java.util.List;

/**
 * SQL Repository for Order operations.
 * 
 * Extends FastRepository for automatic CRUD operations.
 * No implementation needed - the framework creates a dynamic proxy at runtime.
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

    // Custom queries:
    @Select("SELECT * FROM orders WHERE customer_id = :customerId")
    List<Order> findByCustomerId(@Param("customerId") String customerId);

    @Select("SELECT * FROM orders WHERE status = :status")
    List<Order> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM orders WHERE customer_id = :customerId AND status = :status")
    List<Order> findByCustomerIdAndStatus(
            @Param("customerId") String customerId,
            @Param("status") String status);
}
