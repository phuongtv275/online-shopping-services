package com.example.notificationservice.consumer;

import com.example.notificationservice.event.ShippingStatusEvent;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingNotificationConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ShippingNotificationConsumer shippingNotificationConsumer;

    @Test
    void handleShippingStatusEvent_WhenDelivered_ShouldSendNotification() {
        ShippingStatusEvent event = ShippingStatusEvent.builder()
                .shippingId(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .status("DELIVERED")
                .shipperName("Nguyen Van Shipper")
                .shipperPhone("0987654321")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .deliveredAt(Instant.now())
                .build();

        shippingNotificationConsumer.handleShippingStatusEvent(event, "test-cid-1");

        verify(notificationService, times(1)).sendDeliverySuccessNotification(event);
    }

    @Test
    void handleShippingStatusEvent_WhenNotDelivered_ShouldNotSendNotification() {
        ShippingStatusEvent event = ShippingStatusEvent.builder()
                .shippingId(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .status("PREPARING")
                .build();

        shippingNotificationConsumer.handleShippingStatusEvent(event, null);

        verify(notificationService, never()).sendDeliverySuccessNotification(any());
    }
}
