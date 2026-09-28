package com.example.orderservice.consumer;

import com.example.orderservice.event.ShippingStatusEvent;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingEventConsumerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private ShippingEventConsumer shippingEventConsumer;

    @Test
    void handleShippingStatusEvent_WhenDelivered_ShouldCallCompleteOrder() {
        ShippingStatusEvent event = ShippingStatusEvent.builder()
                .shippingId(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .status("DELIVERED")
                .deliveredAt(Instant.now())
                .correlationId("test-cid-1")
                .build();

        shippingEventConsumer.handleShippingStatusEvent(event, "test-cid-1");

        verify(orderService, times(1)).completeOrder(101L, "SHIP-20260928-ABCD1234");
    }

    @Test
    void handleShippingStatusEvent_WhenNotDelivered_ShouldNotCallCompleteOrder() {
        ShippingStatusEvent event = ShippingStatusEvent.builder()
                .shippingId(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .status("IN_TRANSIT")
                .build();

        shippingEventConsumer.handleShippingStatusEvent(event, null);

        verify(orderService, never()).completeOrder(anyLong(), anyString());
    }
}
