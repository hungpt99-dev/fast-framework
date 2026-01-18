package com.example.order;

import com.fast.cqrs.autoconfigure.EnableFast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Example application demonstrating Fast Framework with DX features.
 * 
 * Uses @EnableFast for zero-config setup.
 */
import com.fast.cqrs.concurrent.spring.EnableFastConcurrent;

@SpringBootApplication
@EnableFast
@EnableFastConcurrent
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

}
