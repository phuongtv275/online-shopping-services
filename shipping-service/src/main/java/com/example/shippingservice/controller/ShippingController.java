package com.example.shippingservice.controller;

import com.example.shippingservice.dto.ApiResponse;
import com.example.shippingservice.dto.CreateShippingRequest;
import com.example.shippingservice.dto.DeliverShippingRequest;
import com.example.shippingservice.dto.PageResponse;
import com.example.shippingservice.dto.ShippingResponse;
import com.example.shippingservice.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shippings")
@RequiredArgsConstructor
@Slf4j
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShippingResponse>> createShipping(
            @Valid @RequestBody CreateShippingRequest request) {
        log.info("REST: Tiếp nhận yêu cầu tạo vận đơn mới cho orderId: {}", request.getOrderId());
        ShippingResponse response = shippingService.createShipping(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Vận đơn đã được tạo thành công"));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<ShippingResponse>> deliverShipping(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) DeliverShippingRequest request) {
        log.info("REST: Shipper gửi yêu cầu xác nhận giao hàng cho vận đơn ID: {}", id);
        ShippingResponse response = shippingService.deliverShipping(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Xác nhận giao hàng thành công và đã phát sự kiện thông báo"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingResponse>> getShippingById(@PathVariable("id") Long id) {
        log.info("REST: Tra cứu thông tin vận đơn ID: {}", id);
        ShippingResponse response = shippingService.getShippingById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin vận đơn thành công"));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<ShippingResponse>> getShippingByOrderId(@PathVariable("orderId") Long orderId) {
        log.info("REST: Tra cứu thông tin vận đơn theo orderId: {}", orderId);
        ShippingResponse response = shippingService.getShippingByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin vận đơn theo đơn hàng thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ShippingResponse>>> getAllShippings(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST: Lấy danh sách vận đơn phân trang: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        PageResponse<ShippingResponse> response = shippingService.getAllShippings(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách vận đơn thành công"));
    }
}
