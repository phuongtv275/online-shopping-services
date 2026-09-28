package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.DeductStockRequest;
import com.example.inventoryservice.dto.DeductStockResponse;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.ProcessedEvent;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.event.OrderItemDto;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.exception.ResourceNotFoundException;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

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
    void deductStock_WhenStockAvailable_ShouldDeductAndReturnRemaining() {
        Inventory inventory = Inventory.builder().id(1L).productId(1L).quantity(10).reservedQuantity(0).build();
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.saveAndFlush(any(Inventory.class))).thenReturn(inventory);

        DeductStockRequest request = new DeductStockRequest(1L, 3);
        DeductStockResponse response = inventoryService.deductStock(request);

        assertNotNull(response);
        assertEquals(1L, response.getProductId());
        assertEquals(3, response.getDeductedQuantity());
        assertEquals(7, response.getRemainingQuantity());
        assertEquals(7, inventory.getQuantity());
        verify(inventoryRepository).saveAndFlush(inventory);
    }

    @Test
    void deductStock_WhenStockInsufficient_ShouldThrowException() {
        Inventory inventory = Inventory.builder().id(1L).productId(1L).quantity(2).reservedQuantity(0).build();
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        DeductStockRequest request = new DeductStockRequest(1L, 5);
        assertThrows(InsufficientStockException.class, () -> inventoryService.deductStock(request));
        verify(inventoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void deductStock_WhenProductNotFound_ShouldThrowException() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        DeductStockRequest request = new DeductStockRequest(999L, 1);
        assertThrows(ResourceNotFoundException.class, () -> inventoryService.deductStock(request));
    }

    @Test
    void processOrderCreatedEvent_WhenNewEvent_ShouldDeductStockAndSaveProcessedEvent() throws Exception {
        Inventory inventory = Inventory.builder().id(1L).productId(1L).quantity(50).reservedQuantity(0).build();

        when(processedEventRepository.existsById("ORDER_CREATED_101")).thenReturn(false);
        when(redissonClient.getLock("lock:product:1")).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        inventoryService.processOrderCreatedEvent(sampleEvent);

        assertEquals(48, inventory.getQuantity());
        verify(inventoryRepository).save(inventory);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(rLock).unlock();
    }

    @Test
    void processOrderCreatedEvent_WhenAlreadyProcessed_ShouldSkipDeduction() {
        when(processedEventRepository.existsById("ORDER_CREATED_101")).thenReturn(true);

        inventoryService.processOrderCreatedEvent(sampleEvent);

        verify(redissonClient, never()).getLock(anyString());
        verify(inventoryRepository, never()).findByProductId(any());
        verify(inventoryRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }
}
