package com.example.shippingservice.producer;

import com.example.shippingservice.event.ShippingStatusEvent;
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
 * Kafka Producer phát sự kiện ShippingStatusEvent vào topic 'shipping-events'.
 * Tự động gắn header X-Correlation-Id để duy trì distributed tracing trong hệ thống Event-Driven Choreography.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingEventProducer {

    public static final String TOPIC = "shipping-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishShippingStatusEvent(ShippingStatusEvent event) {
        String key = String.valueOf(event.getOrderId());
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = event.getCorrelationId();
        }

        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, key, event);
        if (correlationId != null) {
            record.headers().add(new RecordHeader("X-Correlation-Id", correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        log.info("[CHOREOGRAPHY-PRODUCER] Phát sự kiện ShippingStatusEvent: topic={}, key={}, orderId={}, status={}",
                TOPIC, key, event.getOrderId(), event.getStatus());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[CHOREOGRAPHY-PRODUCER] Gửi sự kiện ShippingStatusEvent thất bại cho orderId {}: {}",
                        event.getOrderId(), ex.getMessage(), ex);
            } else {
                log.info("[CHOREOGRAPHY-PRODUCER] Gửi sự kiện ShippingStatusEvent thành công: orderId={}, status={}, partition={}, offset={}",
                        event.getOrderId(),
                        event.getStatus(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
