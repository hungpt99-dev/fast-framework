package com.example.order;

import com.fast.cqrs.autoconfigure.EnableFast;
import com.fast.cqrs.event.AsyncEventBus;
import com.fast.cqrs.event.EventBus;
import com.fast.cqrs.eventsourcing.EventStore;
import com.fast.cqrs.eventsourcing.InMemoryEventStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Example application demonstrating all Fast Framework features.
 * 
 * Uses @EnableFast for zero-config setup with convention-based scanning.
 */
@SpringBootApplication
@EnableFast  // Single annotation - enables CQRS, SQL, Events, etc.
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    /**
     * Event bus for publishing domain events.
     */
    @Bean
    public EventBus eventBus() {
        return new AsyncEventBus();
    }

    /**
     * Event store for event sourcing (use JdbcEventStore in production).
     */
    @Bean
    public EventStore eventStore() {
        return new InMemoryEventStore();
    }
}
