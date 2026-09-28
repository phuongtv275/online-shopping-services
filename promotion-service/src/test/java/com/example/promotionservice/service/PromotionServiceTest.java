package com.example.promotionservice.service;

import com.example.promotionservice.client.ProductServiceClient;
import com.example.promotionservice.config.RedisConfig;
import com.example.promotionservice.dto.ApiResponse;
import com.example.promotionservice.dto.CreatePromotionRequest;
import com.example.promotionservice.dto.ProductResponse;
import com.example.promotionservice.dto.PromotionResponse;
import com.example.promotionservice.entity.Promotion;
import com.example.promotionservice.event.PromotionUpdatedEvent;
import com.example.promotionservice.exception.ResourceNotFoundException;
import com.example.promotionservice.mapper.PromotionMapper;
import com.example.promotionservice.repository.PromotionRepository;
import com.example.promotionservice.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionMapper promotionMapper;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    private CreatePromotionRequest sampleRequest;
    private Promotion samplePromotion;
    private PromotionResponse sampleResponse;
    private ProductResponse sampleProduct;

    @BeforeEach
    void setUp() {
        sampleRequest = CreatePromotionRequest.builder()
                .productId(1L)
                .promotionName("Giảm giá cuối tuần")
                .discountPercent(20)
                .startDate(Instant.now())
                .endDate(Instant.now().plus(3, ChronoUnit.DAYS))
                .build();

        samplePromotion = Promotion.builder()
                .id(100L)
                .productId(1L)
                .promotionName("Giảm giá cuối tuần")
                .discountPercent(20)
                .startDate(sampleRequest.getStartDate())
                .endDate(sampleRequest.getEndDate())
                .isActive(true)
                .build();

        sampleResponse = PromotionResponse.builder()
                .id(100L)
                .productId(1L)
                .promotionName("Giảm giá cuối tuần")
                .discountPercent(20)
                .isActive(true)
                .build();

        sampleProduct = ProductResponse.builder()
                .id(1L)
                .name("iPhone 16 Pro Max")
                .price(BigDecimal.valueOf(34990000))
                .build();
    }

    @Test
    void createPromotion_WhenProductExists_ShouldSaveAndPublishToRedis() {
        // Mock Feign client trả về sản phẩm hợp lệ
        when(productServiceClient.getProductById(1L))
                .thenReturn(ApiResponse.success(sampleProduct, "Found product"));
        when(promotionMapper.toEntity(sampleRequest)).thenReturn(samplePromotion);
        when(promotionRepository.save(any(Promotion.class))).thenReturn(samplePromotion);
        when(promotionMapper.toDto(samplePromotion)).thenReturn(sampleResponse);

        PromotionResponse result = promotionService.createPromotion(sampleRequest);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(1L, result.getProductId());
        assertEquals(20, result.getDiscountPercent());

        // Verify lưu vào CSDL
        verify(promotionRepository).save(any(Promotion.class));

        // Verify publish qua Redis Channel promotion-updates
        ArgumentCaptor<PromotionUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(PromotionUpdatedEvent.class);
        verify(redisTemplate).convertAndSend(eq(RedisConfig.PROMOTION_UPDATES_CHANNEL), eventCaptor.capture());

        PromotionUpdatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(1L, publishedEvent.getProductId());
        assertEquals(20, publishedEvent.getDiscountPercent());
        assertTrue(publishedEvent.getIsActive());
    }

    @Test
    void createPromotion_WhenProductNotFound_ShouldThrowExceptionAndNotPublish() {
        when(productServiceClient.getProductById(1L))
                .thenReturn(ApiResponse.error(404, "Product not found"));

        assertThrows(ResourceNotFoundException.class, () -> promotionService.createPromotion(sampleRequest));

        verify(promotionRepository, never()).save(any());
        verify(redisTemplate, never()).convertAndSend(anyString(), any());
    }

    @Test
    void deactivatePromotion_WhenExists_ShouldUpdateStatusAndPublishDeactivationEvent() {
        when(promotionRepository.findById(100L)).thenReturn(Optional.of(samplePromotion));

        promotionService.deactivatePromotion(100L);

        assertFalse(samplePromotion.getIsActive());
        verify(promotionRepository).save(samplePromotion);

        ArgumentCaptor<PromotionUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(PromotionUpdatedEvent.class);
        verify(redisTemplate).convertAndSend(eq(RedisConfig.PROMOTION_UPDATES_CHANNEL), eventCaptor.capture());

        PromotionUpdatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(1L, publishedEvent.getProductId());
        assertFalse(publishedEvent.getIsActive());
        assertEquals(0, publishedEvent.getDiscountPercent());
    }
}
