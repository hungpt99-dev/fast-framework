package com.example.order.handler;

import com.example.order.dto.GetOrderQuery;
import com.example.order.dto.OrderDto;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.handler.QueryHandler;
import com.fast.cqrs.logging.annotation.TraceLog;

import org.springframework.stereotype.Component;

/**
 * Handler for GetOrderQuery.
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
        return orderRepository.findById(query.id());
    }
}
