package com.example.promotionservice.controller;

import com.example.promotionservice.dto.ApiResponse;
import com.example.promotionservice.dto.CreatePromotionRequest;
import com.example.promotionservice.dto.PageResponse;
import com.example.promotionservice.dto.PromotionResponse;
import com.example.promotionservice.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request) {
        log.info("Nhận yêu cầu tạo khuyến mãi: productId={}, discountPercent={}%",
                request.getProductId(), request.getDiscountPercent());
        PromotionResponse response = promotionService.createPromotion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Tạo chương trình khuyến mãi thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PromotionResponse>>> getAllPromotions(
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        log.info("Lấy danh sách khuyến mãi: activeOnly={}, page={}, size={}",
                activeOnly, pageable.getPageNumber(), pageable.getPageSize());
        PageResponse<PromotionResponse> response = promotionService.getAllPromotions(activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách khuyến mãi thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionById(@PathVariable Long id) {
        log.info("Lấy chi tiết khuyến mãi theo ID: {}", id);
        PromotionResponse response = promotionService.getPromotionById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết khuyến mãi thành công"));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getActivePromotionByProductId(
            @PathVariable Long productId) {
        log.info("Lấy khuyến mãi đang hiệu lực cho sản phẩm ID: {}", productId);
        PromotionResponse response = promotionService.getActivePromotionByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy khuyến mãi sản phẩm thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivatePromotion(@PathVariable Long id) {
        log.info("Hủy kích hoạt khuyến mãi ID: {}", id);
        promotionService.deactivatePromotion(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Hủy kích hoạt khuyến mãi thành công"));
    }
}
