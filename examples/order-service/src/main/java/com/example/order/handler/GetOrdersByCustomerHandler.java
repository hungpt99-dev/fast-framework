package com.example.order.handler;

import com.example.order.dto.GetOrdersByCustomerQuery;
import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.handler.QueryHandler;
import com.fast.cqrs.logging.annotation.TraceLog;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handler for GetOrdersByCustomerQuery.
 * <p>
 * Registered automatically with QueryBus.
 * Controller uses @Query without handler - framework auto-dispatches by query
 * type.
 */
@Component
public class GetOrdersByCustomerHandler implements QueryHandler<GetOrdersByCustomerQuery, List<OrderDto>> {

    private final OrderRepository orderRepository;

    public GetOrdersByCustomerHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @TraceLog(slowMs = 200)
    @Override
    public List<OrderDto> handle(GetOrdersByCustomerQuery query) {
        // Use pagination parameters from query
        List<Order> orders;

        if (query.status() != null) {
            orders = orderRepository.findByCustomerIdAndStatus(
                    query.customerId(),
                    query.status());
        } else {
            orders = orderRepository.findByCustomerId(query.customerId());
        }

        // TODO: Apply pagination (page, size) and sorting (sort)
        // For now, just return all matching orders

        return orders.stream()
                .map(OrderDto::fromEntity)
                .toList();
    }
}
