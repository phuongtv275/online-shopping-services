package com.example.inventoryservice.consumer;

import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka Consumer lắng nghe topic 'order-events' thuộc consumer group 'inventory-order-group'.
 * Tự động đồng bộ correlationId từ Kafka header vào MDC log.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "order-events", groupId = "inventory-order-group")
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
            log.info("Nhận sự kiện OrderCreatedEvent từ Kafka: orderId={}, tổng tiền={}",
                    event.getOrderId(), event.getTotalAmount());
            inventoryService.processOrderCreatedEvent(event);
            log.info("Hoàn tất xử lý trừ kho cho đơn hàng ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Lỗi khi xử lý sự kiện OrderCreatedEvent cho orderId {}: {}", event.getOrderId(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
