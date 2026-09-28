package com.example.orderservice.event;

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
public class OrderCreatedEvent implements Serializable {

    private Long orderId;
    private Long customerId;
    private String customerEmail;
    private String shippingAddress;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private String status;
    private String correlationId;
    private Instant createdAt;
}
