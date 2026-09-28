package com.example.productservice.service.impl;

import com.example.productservice.dto.PageResponse;
import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.entity.Product;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * Logic Cache-Aside Pattern (Exercise 01):
     * Khi gọi getProductById, Spring Cache sẽ kiểm tra trong Redis với key "products::{id}".
     * - Cache Hit: Trả trực tiếp dữ liệu từ Redis (<10ms), hàm này KHÔNG được thực thi.
     * - Cache Miss: Hàm này được gọi để truy vấn PostgreSQL (~200ms), kết quả sau đó
     *   được tự động lưu vào Redis với TTL là 30 phút theo cấu hình RedisCacheManager.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.info("⚠️ [CACHE MISS] Đang truy vấn trực tiếp từ PostgreSQL cho Product ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(String category, Pageable pageable) {
        log.info("Lấy danh sách sản phẩm phân trang: page={}, size={}, category={}",
                pageable.getPageNumber(), pageable.getPageSize(), category);
        Page<Product> productPage;
        if (category != null && !category.isBlank()) {
            productPage = productRepository.findByCategoryIgnoreCase(category, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        Page<ProductResponse> responsePage = productPage.map(product -> ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build());
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Tạo mới sản phẩm: {}", request.getName());
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .build();
        Product saved = productRepository.save(product);
        return ProductResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .price(saved.getPrice())
                .stockQuantity(saved.getStockQuantity())
                .imageUrl(saved.getImageUrl())
                .category(saved.getCategory())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    /**
     * Cache Invalidation Logic (Exercise 01):
     * Khi thông tin sản phẩm bị cập nhật bởi quản trị viên, annotation @CacheEvict
     * sẽ tự động xóa key "products::{id}" khỏi Redis để đảm bảo tính nhất quán (Consistency).
     * Lần truy vấn tiếp theo buộc phải đọc dữ liệu mới nhất từ Database.
     */
    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("🔄 [CACHE EVICT] Cập nhật sản phẩm ID: {}, tiến hành xóa cache Redis", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());

        Product updated = productRepository.save(product);
        return ProductResponse.builder()
                .id(updated.getId())
                .name(updated.getName())
                .description(updated.getDescription())
                .price(updated.getPrice())
                .stockQuantity(updated.getStockQuantity())
                .imageUrl(updated.getImageUrl())
                .category(updated.getCategory())
                .createdAt(updated.getCreatedAt())
                .updatedAt(updated.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        log.info("🗑️ [CACHE EVICT] Xóa sản phẩm ID: {}, xóa cache Redis", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id);
        }
        productRepository.deleteById(id);
    }
}
