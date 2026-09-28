package com.example.productservice.service;

import com.example.productservice.ProductServiceApplication;
import com.example.productservice.config.RedisConfig;
import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.entity.Product;
import com.example.productservice.event.PromotionUpdatedEvent;
import com.example.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
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
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @MockitoBean
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        var cache = cacheManager.getCache(RedisConfig.PRODUCT_CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
        try (RedisConnection conn = connectionFactory.getConnection()) {
            conn.serverCommands().flushDb();
        }
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

        ProductResponse response1 = productService.getProductById(100L);
        assertNotNull(response1);
        verify(productRepository, times(1)).findById(100L);

        ProductResponse response2 = productService.getProductById(100L);
        assertNotNull(response2);
        verify(productRepository, times(1)).findById(100L);
    }

    @Test
    @DisplayName("Kiểm thử @CacheEvict: Khi cập nhật sản phẩm, cache Redis phải tự động bị xóa")
    void testCacheEvict_OnUpdateProduct() throws Exception {
        Long evictProductId = 300L;
        Product evictProduct = Product.builder()
                .id(evictProductId)
                .name("MacBook Pro M3")
                .description("Laptop Apple")
                .price(BigDecimal.valueOf(45000000))
                .stockQuantity(10)
                .category("LAPTOP")
                .build();
        when(productRepository.findById(evictProductId)).thenReturn(Optional.of(evictProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.getProductById(evictProductId);
        verify(productRepository, times(1)).findById(evictProductId);

        productService.getProductById(evictProductId);
        verify(productRepository, times(1)).findById(evictProductId);

        ProductRequest updateRequest = ProductRequest.builder()
                .name("MacBook Pro M3 Max")
                .price(BigDecimal.valueOf(60000000))
                .stockQuantity(8)
                .category("LAPTOP")
                .build();
        productService.updateProduct(evictProductId, updateRequest);

        // Chờ Redis hoàn tất xóa cache
        boolean cacheEvicted = false;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 1000) {
            if (Boolean.FALSE.equals(redisTemplate.hasKey("products::" + evictProductId))) {
                cacheEvicted = true;
                break;
            }
            Thread.sleep(20);
        }
        assertTrue(cacheEvicted, "Cache key phải bị xóa khỏi Redis sau khi cập nhật sản phẩm");

        ProductResponse response3 = productService.getProductById(evictProductId);
        assertNotNull(response3);
        verify(productRepository, times(3)).findById(evictProductId);
    }

    @Test
    @DisplayName("Kiểm thử Redis Pub/Sub: Khi có khuyến mãi, cache sản phẩm bị xóa tự động trong chưa đầy 1 giây (<1000ms)")
    void testPromotionPubSub_ShouldEvictProductCacheUnderOneSecond() throws Exception {
        Long promoProductId = 200L;
        Product promoProduct = Product.builder()
                .id(promoProductId)
                .name("Samsung Galaxy S24 Ultra")
                .price(BigDecimal.valueOf(29990000))
                .stockQuantity(15)
                .category("SMARTPHONE")
                .build();
        when(productRepository.findById(promoProductId)).thenReturn(Optional.of(promoProduct));

        // 1. Lần 1: Gọi lấy thông tin sản phẩm để nạp vào Redis Cache
        ProductResponse res1 = productService.getProductById(promoProductId);
        assertNotNull(res1);
        verify(productRepository, times(1)).findById(promoProductId);

        String cacheKey = RedisConfig.PRODUCT_CACHE_NAME + "::200";
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey)), "Cache key phải tồn tại trong Redis sau lần gọi đầu");

        // 2. Publish sự kiện vào channel 'promotion-updates'
        PromotionUpdatedEvent event = PromotionUpdatedEvent.builder()
                .productId(promoProductId)
                .promotionName("Siêu sale giảm 30%")
                .discountPercent(30)
                .isActive(true)
                .correlationId("test-cid-pubsub-realtime")
                .timestamp(Instant.now())
                .build();

        long startTime = System.currentTimeMillis();
        redisTemplate.convertAndSend(RedisConfig.PROMOTION_UPDATES_CHANNEL, event);

        // 3. Đợi và kiểm tra cache bị xóa trong vòng tối đa 1000ms (< 1 giây)
        boolean cacheEvicted = false;
        long elapsedTime = 0;

        while (elapsedTime < 1000) {
            Boolean keyExists = redisTemplate.hasKey(cacheKey);
            if (Boolean.FALSE.equals(keyExists)) {
                cacheEvicted = true;
                break;
            }
            Thread.sleep(20);
            elapsedTime = System.currentTimeMillis() - startTime;
        }

        System.out.println("====== KẾT QUẢ KIỂM THỬ THỜI GIAN THỰC REDIS PUB/SUB ======");
        System.out.println("Thời gian từ khi publish đến khi cache bị xóa: " + elapsedTime + " ms");
        System.out.println("Trạng thái xóa cache thành công: " + cacheEvicted);

        assertTrue(cacheEvicted, "Cache key phải bị xóa khỏi Redis sau khi nhận Pub/Sub event");
        assertTrue(elapsedTime < 1000, "Thời gian xóa cache phải dưới 1 giây (<1000ms), thực tế: " + elapsedTime + " ms");

        // 4. Lần 2: Gọi lại productService.getProductById -> Buộc query lại DB (Cache Miss)
        ProductResponse res2 = productService.getProductById(promoProductId);
        assertNotNull(res2);
        verify(productRepository, times(2)).findById(promoProductId);
    }
}
