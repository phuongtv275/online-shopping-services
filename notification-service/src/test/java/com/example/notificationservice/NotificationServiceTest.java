package com.example.notificationservice;

import com.example.notificationservice.event.OrderCreatedEvent;
import com.example.notificationservice.event.OrderItemDto;
import com.example.notificationservice.service.NotificationService;
import com.example.notificationservice.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationServiceImpl();

    @Test
    void sendOrderConfirmationNotification_ShouldExecuteWithoutException() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(202L)
                .customerId(10L)
                .customerEmail("customer@example.com")
                .shippingAddress("456 Nguyen Trai, Ha Noi")
                .totalAmount(new BigDecimal("35000000.00"))
                .status("PENDING")
                .createdAt(Instant.now())
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(1L)
                                .productName("iPhone 16 Pro Max")
                                .quantity(1)
                                .unitPrice(new BigDecimal("35000000.00"))
                                .build()
                ))
                .build();

        assertDoesNotThrow(() -> notificationService.sendOrderConfirmationNotification(event));
    }

    @Test
    void sendDeliverySuccessNotification_ShouldExecuteWithoutException() {
        com.example.notificationservice.event.ShippingStatusEvent event =
                com.example.notificationservice.event.ShippingStatusEvent.builder()
                        .shippingId(1L)
                        .orderId(202L)
                        .trackingNumber("SHIP-20260928-ABCD1234")
                        .status("DELIVERED")
                        .shipperName("Nguyen Van Shipper")
                        .shipperPhone("0987654321")
                        .shippingAddress("456 Nguyen Trai, Ha Noi")
                        .deliveredAt(Instant.now())
                        .build();

        assertDoesNotThrow(() -> notificationService.sendDeliverySuccessNotification(event));
    }
}
