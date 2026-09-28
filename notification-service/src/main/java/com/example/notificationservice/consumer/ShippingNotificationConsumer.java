package com.example.notificationservice.consumer;

import com.example.notificationservice.event.ShippingStatusEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka Consumer lắng nghe topic 'shipping-events' thuộc consumer group 'notification-shipping-group'.
 * Tiêu thụ sự kiện độc lập song song với Order Service (Choreography Fan-out).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingNotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "shipping-events",
            groupId = "notification-shipping-group",
            containerFactory = "shippingKafkaListenerContainerFactory"
    )
    public void handleShippingStatusEvent(
            @Payload ShippingStatusEvent event,
            @Header(name = "X-Correlation-Id", required = false) String correlationIdHeader) {

        String correlationId = correlationIdHeader;
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = event.getCorrelationId();
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put("correlationId", correlationId);
        try {
            log.info("[CHOREOGRAPHY-NOTIFICATION] Nhận sự kiện ShippingStatusEvent: orderId={}, status={}, trackingNumber={}",
                    event.getOrderId(), event.getStatus(), event.getTrackingNumber());

            if ("DELIVERED".equalsIgnoreCase(event.getStatus())) {
                notificationService.sendDeliverySuccessNotification(event);
            } else {
                log.info("[CHOREOGRAPHY-NOTIFICATION] Bỏ qua sự kiện không phải trạng thái DELIVERED: {}", event.getStatus());
            }
        } catch (Exception e) {
            log.error("[CHOREOGRAPHY-NOTIFICATION] Lỗi khi xử lý thông báo giao hàng cho đơn hàng ID {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
