package com.example.orderservice.service;

import com.example.orderservice.dto.OrderCreateRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(OrderCreateRequest request);

    OrderResponse getOrderById(Long id);

    PageResponse<OrderResponse> getAllOrders(Pageable pageable);
}
