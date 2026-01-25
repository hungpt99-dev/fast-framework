package com.example.order.service;

import com.example.order.dto.OrderDto;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.concurrent.flow.FlowResult;
import com.fast.cqrs.concurrent.flow.ParallelFlow;
import com.fast.cqrs.concurrent.task.Tasks;
import com.fast.cqrs.logging.annotation.TraceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service demonstrating parallel execution features.
 * <p>
 * Features demonstrated:
 * <ul>
 *   <li>{@link ParallelFlow} for fan-out/fan-in operations</li>
 *   <li>{@link Tasks} API for async with timeout/retry/fallback</li>
 *   <li>Context propagation to child threads</li>
 * </ul>
 */
@Service
public class OrderEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(OrderEnrichmentService.class);

    private final OrderRepository orderRepository;

    public OrderEnrichmentService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Load order with enriched data using parallel execution.
     * <p>
     * Uses ParallelFlow to fetch order, customer info, and inventory in parallel.
     */
    @TraceLog(slowMs = 200)
    public EnrichedOrder getEnrichedOrder(String orderId) {
        FlowResult result = ParallelFlow.of()
                .task("order", () -> fetchOrder(orderId))
                .task("customer", () -> fetchCustomerInfo(orderId))
                .task("inventory", () -> checkInventory(orderId))
                .timeout(3, TimeUnit.SECONDS)
                .failFast()
                .execute();

        if (result.hasErrors()) {
            result.errors().forEach((task, error) -> 
                log.error("Task {} failed: {}", task, error.getMessage()));
            throw new RuntimeException("Failed to enrich order: " + orderId);
        }

        return new EnrichedOrder(
            result.get("order"),
            result.get("customer"),
            result.get("inventory")
        );
    }

    /**
     * Process multiple orders in parallel with bounded concurrency.
     */
    @TraceLog(slowMs = 500)
    public List<OrderDto> processOrders(List<String> orderIds) {
        return com.fast.cqrs.concurrent.stream.ParallelStream.from(orderIds)
                .parallel(5)  // Max 5 concurrent operations
                .timeoutPerItem(1, TimeUnit.SECONDS)
                .retryPerItem(2)
                .skipOnError()
                .map(this::fetchOrder) // Map must come after configuration
                .collect();
    }

    /**
     * Async task with timeout, retry, and fallback.
     */
    public OrderDto getOrderWithFallback(String orderId) {
        return Tasks.supply("get-order-" + orderId, () -> fetchOrder(orderId))
                .timeout(2, TimeUnit.SECONDS)
                .retry(3)
                .fallback(() -> createDefaultOrder(orderId))
                .execute();
    }

    // Simulated external service calls
    private OrderDto fetchOrder(String orderId) {
        log.debug("Fetching order: {}", orderId);
        return orderRepository.findById(orderId)
                .map(o -> new OrderDto(o.getId(), o.getCustomerId(), o.getTotal(), o.getStatus(), o.getCreatedAt()))
                .orElse(null);
    }

    private CustomerInfo fetchCustomerInfo(String orderId) {
        log.debug("Fetching customer info for order: {}", orderId);
        // Simulated external call
        return new CustomerInfo("CUST-001", "John Doe", "john@example.com");
    }

    private InventoryStatus checkInventory(String orderId) {
        log.debug("Checking inventory for order: {}", orderId);
        // Simulated external call
        return new InventoryStatus(true, 100);
    }

    private OrderDto createDefaultOrder(String orderId) {
        return new OrderDto(orderId, "UNKNOWN", BigDecimal.ZERO, "FALLBACK", null);
    }

    // Inner classes for enriched data
    public record EnrichedOrder(OrderDto order, CustomerInfo customer, InventoryStatus inventory) {}
    public record CustomerInfo(String id, String name, String email) {}
    public record InventoryStatus(boolean available, int quantity) {}
}
