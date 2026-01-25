package com.example.order.controller;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeatureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderRepository orderRepository;

    @Test
    void testCommandIdempotency() throws Exception {
        String requestId = UUID.randomUUID().toString();
        String orderId = "idempotent-order-" + requestId;
        CreateOrderCmd cmd = new CreateOrderCmd(requestId, orderId, "cust-1", new BigDecimal("100.00"));

        // 1. First execution
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk());

        // 2. Second execution (should be idempotent)
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk());

        // Verify repository save called only once
        verify(orderRepository, times(1)).save(any());
    }

    @Test
    void testQueryCaching() throws Exception {
        String orderId = "cache-test-id";
        
        // Mock repository response
        Order mockOrder = new Order();
        mockOrder.setId(orderId);
        mockOrder.setCustomerId("cust-1");
        mockOrder.setTotal(new BigDecimal("50.00"));
        mockOrder.setStatus("CREATED");
        mockOrder.setCreatedAt(java.time.LocalDateTime.now().toString());
        
        when(orderRepository.findById(orderId)).thenReturn(java.util.Optional.of(mockOrder));

        // 1. First query (hits DB/Repo)
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));

        // 2. Second query (hits Cache)
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));

        // Verify repository findById called only once
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void testBeanValidation() throws Exception {
        // Invalid command: negative total
        CreateOrderCmd cmd = new CreateOrderCmd(UUID.randomUUID().toString(), "invalid-order", "cust-1", new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isBadRequest()); // Expect 400
    }
}
