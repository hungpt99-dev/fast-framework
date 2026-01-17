package com.example.order.handler;

import com.example.order.dto.GetOrderQuery;
import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.cqrs.QueryHandler;
import com.fast.cqrs.cqrs.context.QueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler for GetOrderQuery with caching.
 */
@Component
public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderDto> {

    private static final Logger log = LoggerFactory.getLogger(GetOrderHandler.class);
    
    // Simple in-memory cache for demonstration
    private final Map<String, OrderDto> cache = new ConcurrentHashMap<>();
    
    private final OrderRepository orderRepository;

    public GetOrderHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderDto preQuery(GetOrderQuery query, QueryContext ctx) {
        log.info("Checking cache for order: {}", query.id());
        if (cache.containsKey(query.id())) {
            log.info("Cache hit for order: {}", query.id());
            return cache.get(query.id());
        }
        return null;
    }

    @Override
    public OrderDto handle(GetOrderQuery query) {
        log.info("Fetching order from DB: {}", query.id());
        return orderRepository.findById(query.id())
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Order not found: " + query.id()));
    }

    @Override
    public void postQuery(GetOrderQuery query, OrderDto result, QueryContext ctx) {
        if (!ctx.isCacheHit()) {
            log.info("Caching result for order: {}", query.id());
            cache.put(query.id(), result);
        }
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(
            order.getId(),
            order.getCustomerId(),
            order.getTotal(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
}
