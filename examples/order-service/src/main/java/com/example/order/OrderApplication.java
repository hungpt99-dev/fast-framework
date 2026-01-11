package com.example.order;

import com.fast.cqrs.autoconfigure.EnableCqrs;
import com.fast.cqrs.event.AsyncEventBus;
import com.fast.cqrs.event.EventBus;
import com.fast.cqrs.eventsourcing.EventStore;
import com.fast.cqrs.eventsourcing.InMemoryEventStore;
import com.fast.cqrs.sql.autoconfigure.EnableSqlRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Example application demonstrating all Fast Framework features:
 * - CQRS Controllers
 * - SQL Repositories
 * - Event-Driven & Event Sourcing
 * - Virtual Threads
 */
@SpringBootApplication
@EnableCqrs(basePackages = "com.example.order.controller")
@EnableSqlRepositories(basePackages = "com.example.order.repository")
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
