package com.example.orderservice.service;

import com.example.orderservice.client.ProductServiceClient;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.producer.OrderEventProducer;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderCreateRequest request;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        request = OrderCreateRequest.builder()
                .customerId(1L)
                .customerEmail("customer@example.com")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .items(List.of(
                        OrderItemRequest.builder().productId(1L).quantity(2).build()
                ))
                .build();

        productResponse = ProductResponse.builder()
                .id(1L)
                .name("iPhone 16 Pro Max")
                .price(new BigDecimal("35000000.00"))
                .stockQuantity(50)
                .build();
    }

    @Test
    void createOrder_WhenProductExists_ShouldSaveOrderAndPublishKafkaEvent() {
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success(productResponse, "Success");
        when(productServiceClient.getProductById(1L)).thenReturn(apiResponse);

        Order savedOrder = Order.builder()
                .id(101L)
                .customerId(1L)
                .customerEmail("customer@example.com")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .totalAmount(new BigDecimal("70000000.00"))
                .status("PENDING")
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(101L)
                .totalAmount(new BigDecimal("70000000.00"))
                .build();
        when(orderMapper.toEvent(savedOrder)).thenReturn(event);

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(101L)
                .totalAmount(new BigDecimal("70000000.00"))
                .status("PENDING")
                .build();
        when(orderMapper.toDto(savedOrder)).thenReturn(expectedResponse);

        OrderResponse result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("PENDING", result.getStatus());

        verify(orderRepository).save(any(Order.class));
        verify(orderEventProducer).publishOrderCreatedEvent(event);
    }

    @Test
    void createOrder_WhenProductNotFound_ShouldThrowException() {
        ApiResponse<ProductResponse> apiResponse = ApiResponse.error(404, "Product not found");
        when(productServiceClient.getProductById(1L)).thenReturn(apiResponse);

        assertThrows(ResourceNotFoundException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
        verify(orderEventProducer, never()).publishOrderCreatedEvent(any());
    }

    @Test
    void completeOrder_WhenOrderExistsAndPending_ShouldUpdateToCompleted() {
        Order pendingOrder = Order.builder()
                .id(101L)
                .status("PENDING")
                .build();

        when(orderRepository.findById(101L)).thenReturn(Optional.of(pendingOrder));

        orderService.completeOrder(101L, "SHIP-20260928-ABCD1234");

        assertEquals("COMPLETED", pendingOrder.getStatus());
        verify(orderRepository).save(pendingOrder);
    }

    @Test
    void completeOrder_WhenAlreadyCompleted_ShouldSkipSave() {
        Order completedOrder = Order.builder()
                .id(101L)
                .status("COMPLETED")
                .build();

        when(orderRepository.findById(101L)).thenReturn(Optional.of(completedOrder));

        orderService.completeOrder(101L, "SHIP-20260928-ABCD1234");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void completeOrder_WhenNotFound_ShouldThrowResourceNotFoundException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.completeOrder(999L, "SHIP-123"));
    }
}
