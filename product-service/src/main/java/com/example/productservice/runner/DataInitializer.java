package com.example.productservice.runner;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            log.info("Khởi tạo dữ liệu mẫu cho bảng products...");
            List<Product> sampleProducts = List.of(
                    Product.builder()
                            .name("iPhone 16 Pro Max 256GB")
                            .description("Điện thoại thông minh cao cấp Apple A18 Pro")
                            .price(BigDecimal.valueOf(34990000))
                            .stockQuantity(50)
                            .imageUrl("https://example.com/iphone16.jpg")
                            .category("ELECTRONICS")
                            .build(),
                    Product.builder()
                            .name("Samsung Galaxy S24 Ultra")
                            .description("Flagship AI đỉnh cao từ Samsung")
                            .price(BigDecimal.valueOf(29990000))
                            .stockQuantity(40)
                            .imageUrl("https://example.com/s24ultra.jpg")
                            .category("ELECTRONICS")
                            .build(),
                    Product.builder()
                            .name("MacBook Pro M3 14 inch")
                            .description("Laptop chuyên nghiệp dành cho lập trình viên và sáng tạo")
                            .price(BigDecimal.valueOf(42000000))
                            .stockQuantity(25)
                            .imageUrl("https://example.com/macbookm3.jpg")
                            .category("LAPTOP")
                            .build()
            );
            productRepository.saveAll(sampleProducts);
            log.info("Đã tạo {} sản phẩm mẫu thành công!", sampleProducts.size());
        }
    }
}
