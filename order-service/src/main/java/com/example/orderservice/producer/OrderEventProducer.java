package com.example.orderservice.producer;

import com.example.orderservice.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer phát sinh sự kiện OrderCreatedEvent tới topic 'order-events'.
 * Tự động gắn header X-Correlation-Id để duy trì distributed tracing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    public static final String TOPIC = "order-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        String key = String.valueOf(event.getOrderId());
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = event.getCorrelationId();
        }

        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, key, event);
        if (correlationId != null) {
            record.headers().add(new RecordHeader("X-Correlation-Id", correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        log.info("Phát sự kiện OrderCreatedEvent: topic={}, key={}, orderId={}", TOPIC, key, event.getOrderId());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Gửi sự kiện OrderCreatedEvent thất bại cho orderId {}: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            } else {
                log.info("Gửi sự kiện OrderCreatedEvent thành công: orderId={}, partition={}, offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
