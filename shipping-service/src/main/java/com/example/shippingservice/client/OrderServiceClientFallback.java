package com.example.shippingservice.client;

import com.example.shippingservice.dto.ApiResponse;
import com.example.shippingservice.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class OrderServiceClientFallback implements OrderServiceClient {

    @Override
    public ApiResponse<Map<String, Object>> getOrderById(Long id) {
        log.warn("Fallback kích hoạt: Không thể kết nối tới order-service để tra cứu đơn hàng ID: {}", id);
        throw new ServiceUnavailableException("Dịch vụ đơn hàng (order-service) đang tạm thời không khả dụng, vui lòng thử lại sau!");
    }
}
