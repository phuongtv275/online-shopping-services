package com.example.productservice.service;

import com.example.productservice.dto.PageResponse;
import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse getProductById(Long id);
    PageResponse<ProductResponse> getAllProducts(String category, Pageable pageable);
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
}
