package com.example.orderservice.controller;

import com.example.orderservice.config.CorrelationIdFilter;
import com.example.orderservice.dto.*;
import com.example.orderservice.exception.GlobalExceptionHandler;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_WithValidRequest_ShouldReturn201() throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .customerId(1L)
                .customerEmail("customer@example.com")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .items(List.of(
                        OrderItemRequest.builder().productId(1L).quantity(2).build()
                ))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(101L)
                .customerId(1L)
                .customerEmail("customer@example.com")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .totalAmount(new BigDecimal("70000000.00"))
                .status("PENDING")
                .createdAt(Instant.now())
                .items(List.of(
                        OrderItemResponse.builder()
                                .id(1L)
                                .productId(1L)
                                .productName("iPhone 16 Pro Max")
                                .quantity(2)
                                .unitPrice(new BigDecimal("35000000.00"))
                                .build()
                ))
                .build();

        when(orderService.createOrder(any(OrderCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .header("X-Correlation-Id", "test-corr-order-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "test-corr-order-1"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(70000000.00));
    }

    @Test
    void createOrder_WithEmptyItems_ShouldReturn400() throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .customerId(1L)
                .customerEmail("customer@example.com")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .items(Collections.emptyList())
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createOrder_WithInvalidEmail_ShouldReturn400() throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .customerId(1L)
                .customerEmail("not-an-email")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .items(List.of(OrderItemRequest.builder().productId(1L).quantity(1).build()))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getOrderById_WhenExists_ShouldReturn200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(101L)
                .customerId(1L)
                .customerEmail("customer@example.com")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .totalAmount(new BigDecimal("35000000.00"))
                .status("PENDING")
                .build();

        when(orderService.getOrderById(101L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(101));
    }

    @Test
    void getOrderById_WhenNotFound_ShouldReturn404() throws Exception {
        when(orderService.getOrderById(999L))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy đơn hàng ID: 999"));

        mockMvc.perform(get("/api/v1/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getAllOrders_ShouldReturnPagedResponse() throws Exception {
        PageResponse<OrderResponse> pageResponse = PageResponse.<OrderResponse>builder()
                .content(List.of(OrderResponse.builder().id(101L).build()))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(orderService.getAllOrders(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/orders?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
