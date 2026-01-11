package com.example.order.handler;

import com.example.order.dto.GetOrderQuery;
import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.handler.QueryHandler;
import com.fast.cqrs.logging.annotation.TraceLog;

import org.springframework.stereotype.Component;

/**
 * Handler for GetOrderQuery.
 * <p>
 * Registered automatically with QueryBus.
 * Controller uses @Query without handler - framework auto-dispatches by query
 * type.
 */
@Component
public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderDto> {

    private final OrderRepository orderRepository;

    public GetOrderHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @TraceLog(slowMs = 100)
    @Override
    public OrderDto handle(GetOrderQuery query) {
        Order order = orderRepository.findById(query.id()).orElse(null);

        if (order == null) {
            return null;
        }

        // Apply query options
        OrderDto dto = OrderDto.fromEntity(order);

        // TODO: If query.includeItems() or query.includeCustomer(),
        // fetch additional data

        return dto;
    }
}
