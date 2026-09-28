package com.example.promotionservice.controller;

import com.example.promotionservice.config.CorrelationIdFilter;
import com.example.promotionservice.dto.CreatePromotionRequest;
import com.example.promotionservice.dto.PageResponse;
import com.example.promotionservice.dto.PromotionResponse;
import com.example.promotionservice.exception.GlobalExceptionHandler;
import com.example.promotionservice.exception.ResourceNotFoundException;
import com.example.promotionservice.service.PromotionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PromotionController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class PromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PromotionService promotionService;

    @Test
    void createPromotion_WhenValidRequest_ShouldReturn201() throws Exception {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .productId(1L)
                .promotionName("Flash Sale Khủng 20%")
                .discountPercent(20)
                .startDate(Instant.now())
                .endDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        PromotionResponse response = PromotionResponse.builder()
                .id(10L)
                .productId(1L)
                .promotionName("Flash Sale Khủng 20%")
                .discountPercent(20)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(promotionService.createPromotion(any(CreatePromotionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/promotions")
                        .header("X-Correlation-Id", "test-corr-promo-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "test-corr-promo-01"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.discountPercent").value(20))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void createPromotion_WhenInvalidRequest_ShouldReturn400() throws Exception {
        // discountPercent > 100 và productId = null
        CreatePromotionRequest invalidRequest = CreatePromotionRequest.builder()
                .productId(null)
                .promotionName("")
                .discountPercent(150)
                .startDate(null)
                .endDate(null)
                .build();

        mockMvc.perform(post("/api/v1/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu đầu vào không hợp lệ"))
                .andExpect(jsonPath("$.data.productId").exists())
                .andExpect(jsonPath("$.data.discountPercent").exists());
    }

    @Test
    void getAllPromotions_ShouldReturnPagedResponse200() throws Exception {
        PromotionResponse response = PromotionResponse.builder()
                .id(1L)
                .productId(1L)
                .promotionName("Giảm giá mùa hè")
                .discountPercent(15)
                .isActive(true)
                .build();

        PageResponse<PromotionResponse> pageResponse = PageResponse.<PromotionResponse>builder()
                .content(List.of(response))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(promotionService.getAllPromotions(any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/promotions?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].discountPercent").value(15))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getPromotionById_WhenExists_ShouldReturn200() throws Exception {
        PromotionResponse response = PromotionResponse.builder()
                .id(1L)
                .productId(100L)
                .promotionName("Ưu đãi đặc biệt")
                .discountPercent(10)
                .isActive(true)
                .build();

        when(promotionService.getPromotionById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/promotions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productId").value(100));
    }

    @Test
    void getPromotionById_WhenNotFound_ShouldReturn404() throws Exception {
        when(promotionService.getPromotionById(999L))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy khuyến mãi ID: 999"));

        mockMvc.perform(get("/api/v1/promotions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Không tìm thấy khuyến mãi ID: 999"));
    }

    @Test
    void deactivatePromotion_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/v1/promotions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Hủy kích hoạt khuyến mãi thành công"));
    }
}
