package com.example.promotionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse implements Serializable {
    private Long id;
    private Long productId;
    private String promotionName;
    private Integer discountPercent;
    private Instant startDate;
    private Instant endDate;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
