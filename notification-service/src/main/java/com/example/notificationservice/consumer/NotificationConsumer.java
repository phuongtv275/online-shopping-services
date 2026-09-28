package com.example.notificationservice.consumer;

import com.example.notificationservice.event.OrderCreatedEvent;
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
 * Kafka Consumer lắng nghe topic 'order-events' thuộc consumer group 'notification-order-group'.
 * Hoạt động độc lập hoàn toàn với Inventory Consumer (Fan-out Pattern).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order-events", groupId = "notification-order-group")
    public void handleOrderCreatedEvent(
            @Payload OrderCreatedEvent event,
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
            log.info("Nhận sự kiện OrderCreatedEvent để gửi thông báo: orderId={}, customerEmail={}",
                    event.getOrderId(), event.getCustomerEmail());
            notificationService.sendOrderConfirmationNotification(event);
            log.info("Hoàn tất gửi thông báo cho đơn hàng ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Lỗi khi xử lý thông báo đơn hàng ID {}: {}", event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
