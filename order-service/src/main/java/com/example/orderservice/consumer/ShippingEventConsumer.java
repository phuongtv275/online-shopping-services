package com.example.orderservice.consumer;

import com.example.orderservice.event.ShippingStatusEvent;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka Consumer lắng nghe topic 'shipping-events' thuộc consumer group 'order-shipping-group'.
 * Khi nhận trạng thái DELIVERED, tự động cập nhật đơn hàng thành COMPLETED (Choreography Pattern).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "shipping-events", groupId = "order-shipping-group")
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
            log.info("[CHOREOGRAPHY-CONSUMER] Nhận sự kiện ShippingStatusEvent: orderId={}, status={}, trackingNumber={}",
                    event.getOrderId(), event.getStatus(), event.getTrackingNumber());

            if ("DELIVERED".equalsIgnoreCase(event.getStatus())) {
                orderService.completeOrder(event.getOrderId(), event.getTrackingNumber());
            } else {
                log.info("[CHOREOGRAPHY-CONSUMER] Bỏ qua sự kiện không phải trạng thái DELIVERED: {}", event.getStatus());
            }
        } catch (Exception e) {
            log.error("[CHOREOGRAPHY-CONSUMER] Lỗi khi xử lý sự kiện giao hàng cho đơn hàng ID {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
