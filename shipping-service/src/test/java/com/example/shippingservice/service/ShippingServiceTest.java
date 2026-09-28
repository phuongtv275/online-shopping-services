package com.example.shippingservice.service;

import com.example.shippingservice.client.OrderServiceClient;
import com.example.shippingservice.dto.ApiResponse;
import com.example.shippingservice.dto.CreateShippingRequest;
import com.example.shippingservice.dto.DeliverShippingRequest;
import com.example.shippingservice.dto.ShippingResponse;
import com.example.shippingservice.entity.Shipping;
import com.example.shippingservice.event.ShippingStatusEvent;
import com.example.shippingservice.exception.InvalidShippingStateException;
import com.example.shippingservice.exception.ResourceNotFoundException;
import com.example.shippingservice.mapper.ShippingMapper;
import com.example.shippingservice.producer.ShippingEventProducer;
import com.example.shippingservice.repository.ShippingRepository;
import com.example.shippingservice.service.impl.ShippingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ShippingRepository shippingRepository;

    @Mock
    private ShippingMapper shippingMapper;

    @Mock
    private ShippingEventProducer shippingEventProducer;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private ShippingServiceImpl shippingService;

    private CreateShippingRequest createRequest;
    private Shipping mockShipping;
    private ShippingResponse mockResponse;

    @BeforeEach
    void setUp() {
        createRequest = CreateShippingRequest.builder()
                .orderId(101L)
                .shipperName("Nguyen Van Shipper")
                .shipperPhone("0987654321")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .note("Giao gio hanh chinh")
                .build();

        mockShipping = Shipping.builder()
                .id(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .shipperName("Nguyen Van Shipper")
                .shipperPhone("0987654321")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .status("PREPARING")
                .createdAt(Instant.now())
                .build();

        mockResponse = ShippingResponse.builder()
                .id(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .status("PREPARING")
                .build();
    }

    @Test
    void createShipping_WhenOrderExists_ShouldSucceed() {
        // Arrange
        ApiResponse<Map<String, Object>> orderResponse = ApiResponse.success(Map.of("id", 101L), "Success");
        when(orderServiceClient.getOrderById(101L)).thenReturn(orderResponse);
        when(shippingRepository.existsByOrderId(101L)).thenReturn(false);
        when(shippingMapper.toEntity(createRequest)).thenReturn(mockShipping);
        when(shippingRepository.save(any(Shipping.class))).thenReturn(mockShipping);
        when(shippingMapper.toDto(mockShipping)).thenReturn(mockResponse);

        // Act
        ShippingResponse result = shippingService.createShipping(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals(101L, result.getOrderId());
        verify(shippingRepository).save(any(Shipping.class));
    }

    @Test
    void createShipping_WhenOrderDoesNotExist_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(orderServiceClient.getOrderById(101L)).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> shippingService.createShipping(createRequest));
        verify(shippingRepository, never()).save(any(Shipping.class));
    }

    @Test
    void createShipping_WhenShippingAlreadyExistsForOrder_ShouldThrowInvalidShippingStateException() {
        // Arrange
        ApiResponse<Map<String, Object>> orderResponse = ApiResponse.success(Map.of("id", 101L), "Success");
        when(orderServiceClient.getOrderById(101L)).thenReturn(orderResponse);
        when(shippingRepository.existsByOrderId(101L)).thenReturn(true);

        // Act & Assert
        assertThrows(InvalidShippingStateException.class, () -> shippingService.createShipping(createRequest));
        verify(shippingRepository, never()).save(any(Shipping.class));
    }

    @Test
    void deliverShipping_WhenValid_ShouldUpdateStatusAndPublishKafkaEvent() {
        // Arrange
        DeliverShippingRequest deliverRequest = DeliverShippingRequest.builder()
                .deliveryNote("Giao xong")
                .build();

        Shipping deliveredShipping = Shipping.builder()
                .id(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .status("DELIVERED")
                .deliveredAt(Instant.now())
                .build();

        ShippingStatusEvent mockEvent = ShippingStatusEvent.builder()
                .shippingId(1L)
                .orderId(101L)
                .status("DELIVERED")
                .build();

        when(shippingRepository.findById(1L)).thenReturn(Optional.of(mockShipping));
        when(shippingRepository.save(any(Shipping.class))).thenReturn(deliveredShipping);
        when(shippingMapper.toEvent(any(Shipping.class))).thenReturn(mockEvent);
        when(shippingMapper.toDto(deliveredShipping)).thenReturn(
                ShippingResponse.builder().id(1L).status("DELIVERED").build()
        );

        // Act
        ShippingResponse result = shippingService.deliverShipping(1L, deliverRequest);

        // Assert
        assertNotNull(result);
        assertEquals("DELIVERED", result.getStatus());
        verify(shippingEventProducer).publishShippingStatusEvent(mockEvent);
    }

    @Test
    void deliverShipping_WhenAlreadyDelivered_ShouldThrowInvalidShippingStateException() {
        // Arrange
        mockShipping.setStatus("DELIVERED");
        mockShipping.setDeliveredAt(Instant.now());
        when(shippingRepository.findById(1L)).thenReturn(Optional.of(mockShipping));

        // Act & Assert
        assertThrows(InvalidShippingStateException.class, () -> shippingService.deliverShipping(1L, null));
        verify(shippingEventProducer, never()).publishShippingStatusEvent(any());
    }

    @Test
    void deliverShipping_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(shippingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> shippingService.deliverShipping(999L, null));
    }
}
