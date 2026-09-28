package com.example.shippingservice.service;

import com.example.shippingservice.dto.CreateShippingRequest;
import com.example.shippingservice.dto.DeliverShippingRequest;
import com.example.shippingservice.dto.PageResponse;
import com.example.shippingservice.dto.ShippingResponse;
import org.springframework.data.domain.Pageable;

public interface ShippingService {

    ShippingResponse createShipping(CreateShippingRequest request);

    ShippingResponse deliverShipping(Long id, DeliverShippingRequest request);

    ShippingResponse getShippingById(Long id);

    ShippingResponse getShippingByOrderId(Long orderId);

    PageResponse<ShippingResponse> getAllShippings(Pageable pageable);
}
