package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse implements Serializable {

    private Long id;
    private Long customerId;
    private String customerEmail;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemResponse> items;
}
