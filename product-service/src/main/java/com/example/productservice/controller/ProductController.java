package com.example.productservice.controller;

import com.example.productservice.dto.ApiResponse;
import com.example.productservice.dto.PageResponse;
import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.service.ProductService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Endpoint lấy chi tiết sản phẩm:
     * - Tích hợp Resilience4j RateLimiter (productViewLimit) để bảo vệ server khỏi spam/crawler.
     * - Tích hợp Redis Cache-Aside bên trong ProductService.
     */
    @GetMapping("/{id}")
    @RateLimiter(name = "productViewLimit")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        log.info("API Controller: Yêu cầu lấy thông tin sản phẩm ID: {}", id);
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin sản phẩm thành công"));
    }

    /**
     * Endpoint lấy danh sách sản phẩm có phân trang (theo quy chuẩn AGENTS.md):
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = sort[0];
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        PageResponse<ProductResponse> pageResponse = productService.getAllProducts(category, pageable);
        return ResponseEntity.ok(ApiResponse.success(pageResponse, "Lấy danh sách sản phẩm thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        log.info("API Controller: Yêu cầu tạo mới sản phẩm: {}", request.getName());
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, "Tạo sản phẩm mới thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        log.info("API Controller: Yêu cầu cập nhật sản phẩm ID: {}", id);
        ProductResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật sản phẩm thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("API Controller: Yêu cầu xóa sản phẩm ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa sản phẩm thành công"));
    }
}
