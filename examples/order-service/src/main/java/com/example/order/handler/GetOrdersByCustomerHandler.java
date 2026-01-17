package com.example.order.handler;

import com.example.order.dto.GetOrdersByCustomerQuery;
import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.cqrs.QueryHandler;
import com.fast.cqrs.cqrs.context.QueryContext;
import com.fast.cqrs.logging.annotation.TraceLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handler for GetOrdersByCustomerQuery.
 */
@Component
public class GetOrdersByCustomerHandler implements QueryHandler<GetOrdersByCustomerQuery, List<OrderDto>> {

    private static final Logger log = LoggerFactory.getLogger(GetOrdersByCustomerHandler.class);
    private final OrderRepository orderRepository;

    public GetOrdersByCustomerHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<OrderDto> preQuery(GetOrdersByCustomerQuery query, QueryContext ctx) {
        // Validate input
        if (query.customerId() == null || query.customerId().isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        return null;
    }

    @TraceLog(slowMs = 200)
    @Override
    public List<OrderDto> handle(GetOrdersByCustomerQuery query) {
        List<Order> orders;

        if (query.status() != null) {
            orders = orderRepository.findByCustomerIdAndStatus(
                    query.customerId(),
                    query.status());
        } else {
            orders = orderRepository.findByCustomerId(query.customerId());
        }

        return orders.stream()
                .map(OrderDto::fromEntity)
                .toList();
    }
    
    @Override
    public void postQuery(GetOrdersByCustomerQuery query, List<OrderDto> result, QueryContext ctx) {
        log.info("Found {} orders for customer {}", result.size(), query.customerId());
    }
}
