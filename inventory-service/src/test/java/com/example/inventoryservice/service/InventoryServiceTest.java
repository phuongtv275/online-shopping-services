package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.ProcessedEvent;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.event.OrderItemDto;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.mapper.InventoryMapper;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import com.example.inventoryservice.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private OrderCreatedEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = OrderCreatedEvent.builder()
                .orderId(101L)
                .customerId(1L)
                .customerEmail("customer@example.com")
                .shippingAddress("123 Ha Noi")
                .totalAmount(new BigDecimal("70000000.00"))
                .items(List.of(
                        OrderItemDto.builder().productId(1L).productName("iPhone 16").quantity(2).build()
                ))
                .build();
    }

    @Test
    void processOrderCreatedEvent_WhenNewEvent_ShouldDeductStockAndSaveProcessedEvent() {
        Inventory inventory = Inventory.builder().id(1L).productId(1L).quantity(50).reservedQuantity(0).build();

        when(processedEventRepository.existsById("ORDER_CREATED_101")).thenReturn(false);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        inventoryService.processOrderCreatedEvent(sampleEvent);

        assertEquals(48, inventory.getQuantity());
        verify(inventoryRepository).save(inventory);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void processOrderCreatedEvent_WhenAlreadyProcessed_ShouldSkipDeduction() {
        when(processedEventRepository.existsById("ORDER_CREATED_101")).thenReturn(true);

        inventoryService.processOrderCreatedEvent(sampleEvent);

        verify(inventoryRepository, never()).findByProductId(any());
        verify(inventoryRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void processOrderCreatedEvent_WhenInsufficientStock_ShouldThrowException() {
        Inventory inventory = Inventory.builder().id(1L).productId(1L).quantity(1).reservedQuantity(0).build();

        when(processedEventRepository.existsById("ORDER_CREATED_101")).thenReturn(false);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class, () -> inventoryService.processOrderCreatedEvent(sampleEvent));
        verify(processedEventRepository, never()).save(any());
    }
}
