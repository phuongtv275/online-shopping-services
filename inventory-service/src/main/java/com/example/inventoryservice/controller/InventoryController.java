package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.ApiResponse;
import com.example.inventoryservice.dto.DeductStockRequest;
import com.example.inventoryservice.dto.DeductStockResponse;
import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.dto.PageResponse;
import com.example.inventoryservice.service.InventoryService;
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
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProductId(@PathVariable("productId") Long productId) {
        log.info("REST: Tra cứu tồn kho cho productId: {}", productId);
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin tồn kho thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createOrUpdateInventory(@Valid @RequestBody InventoryRequest request) {
        log.info("REST: Nhập kho/cập nhật tồn kho cho productId: {}, quantity: {}", request.getProductId(), request.getQuantity());
        InventoryResponse response = inventoryService.createOrUpdateInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Cập nhật tồn kho thành công"));
    }

    @PostMapping("/deduct")
    public ResponseEntity<ApiResponse<DeductStockResponse>> deductStock(@Valid @RequestBody DeductStockRequest request) {
        log.info("REST: Trừ tồn kho đồng thời với Distributed Lock cho productId: {}, quantity: {}",
                request.getProductId(), request.getQuantity());
        DeductStockResponse response = inventoryService.deductStock(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Trừ tồn kho thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getAllInventories(
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("REST: Danh sách tồn kho phân trang: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        PageResponse<InventoryResponse> response = inventoryService.getAllInventories(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách tồn kho thành công"));
    }
}
