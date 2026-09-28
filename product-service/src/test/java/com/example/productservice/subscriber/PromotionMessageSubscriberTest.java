package com.example.productservice.subscriber;

import com.example.productservice.config.RedisConfig;
import com.example.productservice.event.PromotionUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionMessageSubscriberTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private ObjectMapper objectMapper;

    private PromotionMessageSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        subscriber = new PromotionMessageSubscriber(cacheManager, redisTemplate, objectMapper);
        when(cacheManager.getCache(RedisConfig.PRODUCT_CACHE_NAME)).thenReturn(cache);
    }

    @Test
    void onMessage_WhenValidPromotionEvent_ShouldEvictCacheFromCacheManagerAndRedis() throws Exception {
        PromotionUpdatedEvent event = PromotionUpdatedEvent.builder()
                .productId(10L)
                .promotionName("Giảm giá 25%")
                .discountPercent(25)
                .isActive(true)
                .correlationId("test-cid-pubsub-99")
                .timestamp(Instant.now())
                .build();

        byte[] body = objectMapper.writeValueAsBytes(event);
        byte[] channel = "promotion-updates".getBytes(StandardCharsets.UTF_8);
        Message message = new DefaultMessage(channel, body);

        subscriber.onMessage(message, null);

        // Verify xóa cache trên Spring CacheManager
        verify(cache).evict(10L);

        // Verify xóa trực tiếp key trong RedisTemplate
        verify(redisTemplate).delete("products::10");
    }
}
