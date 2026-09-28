package com.example.productservice.service;

import com.example.productservice.ProductServiceApplication;
import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ProductServiceApplication.class)
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.password=rikkeiacademy",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
class ProductCacheIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @MockitoBean
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Dọn sạch Redis trước mỗi test case để tránh data pollution giữa các test
        connectionFactory.getConnection().serverCommands().flushDb();
        Mockito.reset(productRepository);

        testProduct = Product.builder()
                .id(100L)
                .name("MacBook Pro M3")
                .description("Laptop Apple")
                .price(BigDecimal.valueOf(45000000))
                .stockQuantity(10)
                .category("LAPTOP")
                .build();
    }

    @Test
    @DisplayName("Kiểm thử Cache-aside: Lần 1 đọc DB, lần 2 đọc từ Redis Cache")
    void testCacheAside_FirstCallDb_SecondCallCache() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        // Lần 1: Cache Miss -> Phải truy vấn Database
        ProductResponse response1 = productService.getProductById(100L);
        assertNotNull(response1);
        assertEquals("MacBook Pro M3", response1.getName());
        verify(productRepository, times(1)).findById(100L);

        // Lần 2: Cache Hit -> Đọc trực tiếp từ Redis Cache, KHÔNG được truy vấn Database
        ProductResponse response2 = productService.getProductById(100L);
        assertNotNull(response2);
        assertEquals("MacBook Pro M3", response2.getName());
        verify(productRepository, times(1)).findById(100L); // Vẫn chỉ là 1 lần gọi DB!
    }

    @Test
    @DisplayName("Kiểm thử @CacheEvict: Khi cập nhật sản phẩm, cache Redis phải tự động bị xóa")
    void testCacheEvict_OnUpdateProduct() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Đọc lần 1 để nạp vào cache Redis
        productService.getProductById(100L);
        verify(productRepository, times(1)).findById(100L);

        // Đọc lần 2: Trả về từ Cache
        productService.getProductById(100L);
        verify(productRepository, times(1)).findById(100L);

        // Quản trị viên cập nhật sản phẩm -> Kích hoạt @CacheEvict
        ProductRequest updateRequest = ProductRequest.builder()
                .name("MacBook Pro M3 Max")
                .price(BigDecimal.valueOf(60000000))
                .stockQuantity(8)
                .category("LAPTOP")
                .build();
        productService.updateProduct(100L, updateRequest);

        // Đọc lần 3: Do cache đã bị Evict, buộc phải gọi lại DB lần 3
        ProductResponse response3 = productService.getProductById(100L);
        assertNotNull(response3);
        verify(productRepository, times(3)).findById(100L); // 1 lần ban đầu + 1 lần trong update + 1 lần sau evict
    }
}
