package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.dto.PageResponse;
import com.example.inventoryservice.event.OrderCreatedEvent;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    InventoryResponse getInventoryByProductId(Long productId);

    InventoryResponse createOrUpdateInventory(InventoryRequest request);

    PageResponse<InventoryResponse> getAllInventories(Pageable pageable);

    void processOrderCreatedEvent(OrderCreatedEvent event);
}
