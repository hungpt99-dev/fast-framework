package com.example.order;

import com.fast.cqrs.autoconfigure.EnableCqrs;
import com.fast.cqrs.sql.autoconfigure.EnableSqlRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example application demonstrating Fast Framework usage.
 */
@SpringBootApplication
@EnableCqrs(basePackages = "com.example.order.controller")
@EnableSqlRepositories(basePackages = "com.example.order.repository")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
