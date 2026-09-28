package com.example.productservice.event;

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
public class PromotionUpdatedEvent implements Serializable {
    private Long productId;
    private String promotionName;
    private Integer discountPercent;
    private Boolean isActive;
    private String correlationId;
    private Instant timestamp;
}
