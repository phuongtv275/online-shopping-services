package com.example.orderservice.client;

import com.example.orderservice.dto.ApiResponse;
import com.example.orderservice.dto.ProductResponse;
import com.example.orderservice.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductServiceClientFallback implements ProductServiceClient {

    @Override
    public ApiResponse<ProductResponse> getProductById(Long id) {
        log.error("Fallback kích hoạt cho ProductServiceClient.getProductById({}): product-service tạm thời không phản hồi!", id);
        throw new ServiceUnavailableException("Dịch vụ sản phẩm đang tạm thời bận hoặc không khả dụng. Vui lòng thử lại sau!");
    }
}
