package com.example.shippingservice.client;

import com.example.shippingservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "order-service", fallback = OrderServiceClientFallback.class)
public interface OrderServiceClient {

    @GetMapping("/api/v1/orders/{id}")
    ApiResponse<Map<String, Object>> getOrderById(@PathVariable("id") Long id);
}
