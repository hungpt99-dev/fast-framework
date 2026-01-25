package com.example.order.handler;

import com.example.order.dto.GetOrderQuery;
import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.concurrent.annotation.KeyedConcurrency;
import com.fast.cqrs.cqrs.QueryHandler;
import com.fast.cqrs.cqrs.context.QueryContext;
import com.fast.cqrs.logging.annotation.TraceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler for GetOrderQuery.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Query lifecycle hooks (preQuery, handle, postQuery)</li>
 *   <li>@TraceLog for slow query detection</li>
 *   <li>@KeyedConcurrency for per-key concurrency control</li>
 *   <li>Manual caching in lifecycle hooks</li>
 * </ul>
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

    /**
     * Pre-query: Check cache before hitting database.
     */
    @Override
    public OrderDto preQuery(GetOrderQuery query, QueryContext ctx) {
        log.debug("Checking cache for order: {}", query.id());
        if (cache.containsKey(query.id())) {
            log.debug("Cache hit for order: {}", query.id());
            return cache.get(query.id());
        }
        return null;
    }

    /**
     * Handle: Fetch from database with tracing and concurrency control.
     */
    @Override
    @TraceLog(slowMs = 50)
    @KeyedConcurrency(key = "#query.id")
    public OrderDto handle(GetOrderQuery query) {
        log.debug("Fetching order from DB: {}", query.id());
        return orderRepository.findById(query.id())
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Order not found: " + query.id()));
    }

    /**
     * Post-query: Cache the result for future requests.
     */
    @Override
    public void postQuery(GetOrderQuery query, OrderDto result, QueryContext ctx) {
        if (!ctx.isCacheHit()) {
            log.debug("Caching result for order: {}", query.id());
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

