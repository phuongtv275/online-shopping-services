package com.example.promotionservice.service.impl;

import com.example.promotionservice.client.ProductServiceClient;
import com.example.promotionservice.config.RedisConfig;
import com.example.promotionservice.dto.ApiResponse;
import com.example.promotionservice.dto.CreatePromotionRequest;
import com.example.promotionservice.dto.PageResponse;
import com.example.promotionservice.dto.ProductResponse;
import com.example.promotionservice.dto.PromotionResponse;
import com.example.promotionservice.entity.Promotion;
import com.example.promotionservice.event.PromotionUpdatedEvent;
import com.example.promotionservice.exception.ResourceNotFoundException;
import com.example.promotionservice.mapper.PromotionMapper;
import com.example.promotionservice.repository.PromotionRepository;
import com.example.promotionservice.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final ProductServiceClient productServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public PromotionResponse createPromotion(CreatePromotionRequest request) {
        String correlationId = MDC.get("correlationId");
        log.info("[cid:{}] Bắt đầu tạo khuyến mãi cho sản phẩm ID: {}, giảm giá: {}%",
                correlationId != null ? correlationId : "N/A", request.getProductId(), request.getDiscountPercent());

        // 1. Kiểm tra sự tồn tại của sản phẩm qua OpenFeign ProductServiceClient
        try {
            ApiResponse<ProductResponse> productResponse = productServiceClient.getProductById(request.getProductId());
            if (productResponse == null || productResponse.getData() == null) {
                throw new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + request.getProductId());
            }
            log.info("[cid:{}] Xác thực sản phẩm thành công qua Feign: name='{}', price={}",
                    correlationId != null ? correlationId : "N/A",
                    productResponse.getData().getName(), productResponse.getData().getPrice());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("[cid:{}] Lỗi khi kiểm tra thông tin sản phẩm qua OpenFeign: {}",
                    correlationId != null ? correlationId : "N/A", e.getMessage());
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + request.getProductId());
        }

        // 2. Lưu bản ghi khuyến mãi vào CSDL promotion_db
        Promotion promotion = promotionMapper.toEntity(request);
        promotion.setIsActive(true);
        Promotion savedPromotion = promotionRepository.save(promotion);

        // 3. Đóng gói sự kiện PromotionUpdatedEvent và Publish qua Redis Channel promotion-updates
        PromotionUpdatedEvent event = PromotionUpdatedEvent.builder()
                .productId(savedPromotion.getProductId())
                .promotionName(savedPromotion.getPromotionName())
                .discountPercent(savedPromotion.getDiscountPercent())
                .isActive(true)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        redisTemplate.convertAndSend(RedisConfig.PROMOTION_UPDATES_CHANNEL, event);
        log.info("[cid:{}] [REDIS-PUB] Đã publish sự kiện khuyến mãi cho sản phẩm ID {} vào channel '{}'",
                correlationId != null ? correlationId : "N/A",
                savedPromotion.getProductId(), RedisConfig.PROMOTION_UPDATES_CHANNEL);

        return promotionMapper.toDto(savedPromotion);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponse> getAllPromotions(Boolean activeOnly, Pageable pageable) {
        Page<Promotion> page;
        if (Boolean.TRUE.equals(activeOnly)) {
            page = promotionRepository.findByIsActive(true, pageable);
        } else {
            page = promotionRepository.findAll(pageable);
        }
        return PageResponse.of(page.map(promotionMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khuyến mãi ID: " + id));
        return promotionMapper.toDto(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getActivePromotionByProductId(Long productId) {
        Promotion promotion = promotionRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không có khuyến mãi đang hiệu lực cho sản phẩm ID: " + productId));
        return promotionMapper.toDto(promotion);
    }

    @Override
    @Transactional
    public void deactivatePromotion(Long id) {
        String correlationId = MDC.get("correlationId");
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khuyến mãi ID: " + id));

        promotion.setIsActive(false);
        promotionRepository.save(promotion);

        // Publish event thông báo hủy khuyến mãi để product-service xóa cache
        PromotionUpdatedEvent event = PromotionUpdatedEvent.builder()
                .productId(promotion.getProductId())
                .promotionName(promotion.getPromotionName())
                .discountPercent(0)
                .isActive(false)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        redisTemplate.convertAndSend(RedisConfig.PROMOTION_UPDATES_CHANNEL, event);
        log.info("[cid:{}] [REDIS-PUB-DEACTIVATE] Đã hủy khuyến mãi ID {} và publish event xóa cache",
                correlationId != null ? correlationId : "N/A", id);
    }
}
