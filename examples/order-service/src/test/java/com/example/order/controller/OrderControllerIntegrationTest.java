package com.example.order.controller;

import com.example.order.dto.CreateOrderCmd;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndGetOrder() throws Exception {
        String orderId = UUID.randomUUID().toString();
        String customerId = "cust-123";
        String requestId = UUID.randomUUID().toString();
        CreateOrderCmd cmd = new CreateOrderCmd(requestId, orderId, customerId, new BigDecimal("99.99"));

        // 1. Create Order (POST)
        // This exercises the CommandBus dispatch
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk());

        // 2. Get Order (GET)
        // This exercises the QueryBus dispatch with @ModelAttribute
        // Query param 'includeItems' verifies that parameters are bound to
        // GetOrderQuery object
        mockMvc.perform(get("/api/orders/{id}", orderId)
                .param("includeItems", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.status").value("PENDING")); // Default status from handler
    }

    @Test
    void shouldListOrdersWithPagination() throws Exception {
        // Test pagination params binding via @ModelAttribute to
        // GetOrdersByCustomerQuery
        mockMvc.perform(get("/api/orders")
                .param("customerId", "cust-123")
                .param("page", "0")
                .param("size", "10")
                .param("status", "CREATED"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
