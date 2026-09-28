package com.example.productservice.controller;

import com.example.productservice.config.CorrelationIdFilter;
import com.example.productservice.dto.PageResponse;
import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.exception.GlobalExceptionHandler;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/v1/products/{id} - Thành công trả về 200 và thông tin sản phẩm")
    void testGetProductById_Success() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("iPhone 16 Pro Max")
                .price(BigDecimal.valueOf(34000000))
                .stockQuantity(10)
                .category("ELECTRONICS")
                .build();

        Mockito.when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/1")
                        .header("X-Correlation-Id", "test-cid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("iPhone 16 Pro Max"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Không tìm thấy sản phẩm trả về 404")
    void testGetProductById_NotFound() throws Exception {
        Mockito.when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: 999"));

        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm với ID: 999"));
    }

    @Test
    @DisplayName("GET /api/v1/products - Phân trang danh sách sản phẩm thành công")
    void testGetAllProducts_Pagination() throws Exception {
        ProductResponse item = ProductResponse.builder()
                .id(1L)
                .name("MacBook Pro")
                .price(BigDecimal.valueOf(40000000))
                .stockQuantity(5)
                .category("LAPTOP")
                .build();

        PageResponse<ProductResponse> pageResponse = PageResponse.from(
                new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1)
        );

        Mockito.when(productService.getAllProducts(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].name").value("MacBook Pro"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/products - Dữ liệu không hợp lệ trả về 400 Bad Request")
    void testCreateProduct_ValidationError() throws Exception {
        ProductRequest invalidRequest = ProductRequest.builder()
                .name("") // Blank name
                .price(BigDecimal.valueOf(-100)) // Negative price
                .stockQuantity(-5) // Negative stock
                .category("")
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/products - Hợp lệ trả về 201 Created")
    void testCreateProduct_Success() throws Exception {
        ProductRequest validRequest = ProductRequest.builder()
                .name("Tai nghe Sony WH-1000XM5")
                .price(BigDecimal.valueOf(8000000))
                .stockQuantity(20)
                .category("AUDIO")
                .build();

        ProductResponse created = ProductResponse.builder()
                .id(10L)
                .name("Tai nghe Sony WH-1000XM5")
                .price(BigDecimal.valueOf(8000000))
                .stockQuantity(20)
                .category("AUDIO")
                .build();

        Mockito.when(productService.createProduct(any(ProductRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Cập nhật thành công")
    void testUpdateProduct_Success() throws Exception {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("iPhone 16 Pro Max Updated")
                .price(BigDecimal.valueOf(35000000))
                .stockQuantity(15)
                .category("ELECTRONICS")
                .build();

        ProductResponse updated = ProductResponse.builder()
                .id(1L)
                .name("iPhone 16 Pro Max Updated")
                .price(BigDecimal.valueOf(35000000))
                .stockQuantity(15)
                .category("ELECTRONICS")
                .build();

        Mockito.when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("iPhone 16 Pro Max Updated"));
    }
}
