package com.example.promotionservice.service;

import com.example.promotionservice.dto.CreatePromotionRequest;
import com.example.promotionservice.dto.PageResponse;
import com.example.promotionservice.dto.PromotionResponse;
import org.springframework.data.domain.Pageable;

public interface PromotionService {

    PromotionResponse createPromotion(CreatePromotionRequest request);

    PageResponse<PromotionResponse> getAllPromotions(Boolean activeOnly, Pageable pageable);

    PromotionResponse getPromotionById(Long id);

    PromotionResponse getActivePromotionByProductId(Long productId);

    void deactivatePromotion(Long id);
}
