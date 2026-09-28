package com.example.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductStockResponse implements Serializable {

    private Long productId;
    private Integer deductedQuantity;
    private Integer remainingQuantity;
    private String message;
}
