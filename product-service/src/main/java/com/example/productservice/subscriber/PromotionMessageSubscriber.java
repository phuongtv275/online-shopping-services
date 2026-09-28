package com.example.productservice.subscriber;

import com.example.productservice.config.RedisConfig;
import com.example.productservice.event.PromotionUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub Subscriber trong product-service:
 * Lắng nghe channel 'promotion-updates' khi có chương trình khuyến mãi được tạo hoặc cập nhật.
 * Lập tức xóa cache sản phẩm trong Redis để đảm bảo khách hàng nhìn thấy giá mới nhất ở lần truy vấn kế tiếp.
 */
@Component
@Slf4j
public class PromotionMessageSubscriber implements MessageListener {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public PromotionMessageSubscriber(CacheManager cacheManager,
                                      RedisTemplate<String, Object> redisTemplate,
                                      ObjectMapper objectMapper) {
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        try {
            this.objectMapper.registerModule(new JavaTimeModule());
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            PromotionUpdatedEvent event;
            // Xử lý an toàn nếu body bị double-escaped thành JSON String literal
            if (body.startsWith("\"") && body.endsWith("\"")) {
                String unescaped = objectMapper.readValue(body, String.class);
                event = objectMapper.readValue(unescaped, PromotionUpdatedEvent.class);
            } else {
                event = objectMapper.readValue(body, PromotionUpdatedEvent.class);
            }

            if (event.getCorrelationId() != null && !event.getCorrelationId().isBlank()) {
                MDC.put("correlationId", event.getCorrelationId());
            }

            log.info("[cid:{}] [REDIS-SUB-RECEIVED] Nhận thông điệp từ channel '{}': productId={}, discountPercent={}%",
                    MDC.get("correlationId"), channel, event.getProductId(), event.getDiscountPercent());

            // 1. Xóa cache qua Spring CacheManager
            var cache = cacheManager.getCache(RedisConfig.PRODUCT_CACHE_NAME);
            if (cache != null) {
                cache.evict(event.getProductId());
            }

            // 2. Xóa trực tiếp key trong Redis để đảm bảo sạch cache tuyệt đối
            String redisKey = RedisConfig.PRODUCT_CACHE_NAME + "::" + event.getProductId();
            redisTemplate.delete(redisKey);

            log.info("[cid:{}] [CACHE-EVICTED] Đã xóa cache Redis cho sản phẩm ID: {} (key: '{}') thành công trong <1s",
                    MDC.get("correlationId"), event.getProductId(), redisKey);
        } catch (Exception e) {
            log.error("[cid:{}] Lỗi khi xử lý thông điệp từ Redis Channel '{}': {}",
                    MDC.get("correlationId"), channel, e.getMessage(), e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
