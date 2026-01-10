package com.example.order.repository;

import com.example.order.dto.OrderDto;
import com.fast.cqrs.sql.annotation.Execute;
import com.fast.cqrs.sql.annotation.Param;
import com.fast.cqrs.sql.annotation.Select;
import com.fast.cqrs.sql.annotation.SqlRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SQL Repository for Order operations.
 * 
 * This is an interface-only repository - no implementation needed!
 * The framework creates a dynamic proxy at runtime.
 */
@SqlRepository
public interface OrderRepository {

    @Select("SELECT id, customer_id, total, status, created_at FROM orders WHERE id = :id")
    OrderDto findById(@Param("id") String id);

    @Select("SELECT id, customer_id, total, status, created_at FROM orders WHERE customer_id = :customerId")
    List<OrderDto> findByCustomer(@Param("customerId") String customerId);

    @Select("SELECT id, customer_id, total, status, created_at FROM orders")
    List<OrderDto> findAll();

    @Execute("""
        INSERT INTO orders (id, customer_id, total, status, created_at)
        VALUES (:id, :customerId, :total, :status, :createdAt)
    """)
    void insert(
        @Param("id") String id,
        @Param("customerId") String customerId,
        @Param("total") BigDecimal total,
        @Param("status") String status,
        @Param("createdAt") LocalDateTime createdAt
    );
}
