package com.example.inventoryservice.controller;

import com.example.inventoryservice.config.CorrelationIdFilter;
import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.dto.PageResponse;
import com.example.inventoryservice.exception.GlobalExceptionHandler;
import com.example.inventoryservice.exception.ResourceNotFoundException;
import com.example.inventoryservice.service.InventoryService;
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

@WebMvcTest(InventoryController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void getInventoryByProductId_WhenExists_ShouldReturn200() throws Exception {
        InventoryResponse response = InventoryResponse.builder()
                .id(1L)
                .productId(100L)
                .quantity(50)
                .reservedQuantity(0)
                .updatedAt(Instant.now())
                .build();

        when(inventoryService.getInventoryByProductId(100L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/inventories/100")
                        .header("X-Correlation-Id", "test-corr-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "test-corr-123"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.productId").value(100))
                .andExpect(jsonPath("$.data.quantity").value(50));
    }

    @Test
    void getInventoryByProductId_WhenNotFound_ShouldReturn404() throws Exception {
        when(inventoryService.getInventoryByProductId(999L))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy tồn kho cho sản phẩm ID: 999"));

        mockMvc.perform(get("/api/v1/inventories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createOrUpdateInventory_WithValidData_ShouldReturn201() throws Exception {
        InventoryRequest request = InventoryRequest.builder()
                .productId(1L)
                .quantity(100)
                .build();

        InventoryResponse response = InventoryResponse.builder()
                .id(1L)
                .productId(1L)
                .quantity(100)
                .reservedQuantity(0)
                .updatedAt(Instant.now())
                .build();

        when(inventoryService.createOrUpdateInventory(any(InventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.quantity").value(100));
    }

    @Test
    void createOrUpdateInventory_WithNegativeQuantity_ShouldReturn400() throws Exception {
        InventoryRequest request = InventoryRequest.builder()
                .productId(1L)
                .quantity(-5)
                .build();

        mockMvc.perform(post("/api/v1/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getAllInventories_ShouldReturnPagedResponse() throws Exception {
        PageResponse<InventoryResponse> pageResponse = PageResponse.<InventoryResponse>builder()
                .content(List.of(InventoryResponse.builder().id(1L).productId(1L).quantity(50).build()))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(inventoryService.getAllInventories(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventories?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
