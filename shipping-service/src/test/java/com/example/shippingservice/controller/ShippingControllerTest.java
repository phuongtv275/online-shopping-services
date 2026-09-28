package com.example.shippingservice.controller;

import com.example.shippingservice.config.CorrelationIdFilter;
import com.example.shippingservice.dto.CreateShippingRequest;
import com.example.shippingservice.dto.DeliverShippingRequest;
import com.example.shippingservice.dto.PageResponse;
import com.example.shippingservice.dto.ShippingResponse;
import com.example.shippingservice.exception.GlobalExceptionHandler;
import com.example.shippingservice.exception.InvalidShippingStateException;
import com.example.shippingservice.exception.ResourceNotFoundException;
import com.example.shippingservice.service.ShippingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShippingController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class ShippingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShippingService shippingService;

    @Test
    void createShipping_WithValidRequest_ShouldReturn201() throws Exception {
        CreateShippingRequest request = CreateShippingRequest.builder()
                .orderId(101L)
                .shipperName("Nguyen Van Shipper")
                .shipperPhone("0987654321")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .note("Giao gio hanh chinh")
                .build();

        ShippingResponse response = ShippingResponse.builder()
                .id(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .shipperName("Nguyen Van Shipper")
                .shipperPhone("0987654321")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .status("PREPARING")
                .createdAt(Instant.now())
                .build();

        when(shippingService.createShipping(any(CreateShippingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/shippings")
                        .header("X-Correlation-Id", "test-corr-ship-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "test-corr-ship-1"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderId").value(101))
                .andExpect(jsonPath("$.data.trackingNumber").value("SHIP-20260928-ABCD1234"))
                .andExpect(jsonPath("$.data.status").value("PREPARING"));
    }

    @Test
    void createShipping_WithMissingOrderId_ShouldReturn400() throws Exception {
        CreateShippingRequest request = CreateShippingRequest.builder()
                .shipperName("Nguyen Van Shipper")
                .shipperPhone("0987654321")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .build();

        mockMvc.perform(post("/api/v1/shippings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.orderId").exists());
    }

    @Test
    void createShipping_WithInvalidPhone_ShouldReturn400() throws Exception {
        CreateShippingRequest request = CreateShippingRequest.builder()
                .orderId(101L)
                .shipperName("Nguyen Van Shipper")
                .shipperPhone("invalid-phone")
                .shippingAddress("123 Cau Giay, Ha Noi")
                .build();

        mockMvc.perform(post("/api/v1/shippings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data.shipperPhone").exists());
    }

    @Test
    void deliverShipping_WithValidId_ShouldReturn200() throws Exception {
        DeliverShippingRequest request = DeliverShippingRequest.builder()
                .deliveryNote("Khach da nhan hang thanh cong")
                .build();

        ShippingResponse response = ShippingResponse.builder()
                .id(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .status("DELIVERED")
                .deliveredAt(Instant.now())
                .build();

        when(shippingService.deliverShipping(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/shippings/1/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));
    }

    @Test
    void deliverShipping_WhenAlreadyDelivered_ShouldReturn400() throws Exception {
        when(shippingService.deliverShipping(eq(1L), any()))
                .thenThrow(new InvalidShippingStateException("Vận đơn ID 1 đã được giao thành công trước đó!"));

        mockMvc.perform(post("/api/v1/shippings/1/deliver")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getShippingById_WhenExists_ShouldReturn200() throws Exception {
        ShippingResponse response = ShippingResponse.builder()
                .id(1L)
                .orderId(101L)
                .trackingNumber("SHIP-20260928-ABCD1234")
                .status("PREPARING")
                .build();

        when(shippingService.getShippingById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shippings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getShippingById_WhenNotFound_ShouldReturn404() throws Exception {
        when(shippingService.getShippingById(999L))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy vận đơn với ID: 999"));

        mockMvc.perform(get("/api/v1/shippings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getAllShippings_ShouldReturnPagedResponse() throws Exception {
        PageResponse<ShippingResponse> pageResponse = PageResponse.<ShippingResponse>builder()
                .content(List.of(ShippingResponse.builder().id(1L).orderId(101L).build()))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(shippingService.getAllShippings(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/shippings?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
