package com.example.order.handler;

import com.example.order.dto.GetOrdersByCustomerQuery;
import com.example.order.dto.OrderDto;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.handler.QueryHandler;
import com.fast.cqrs.logging.annotation.TraceLog;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handler for GetOrdersByCustomerQuery.
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
        return orderRepository.findByCustomer(query.customerId());
    }
}
