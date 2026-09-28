package com.example.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductStockRequest implements Serializable {

    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    @NotNull(message = "Số lượng trừ không được để trống")
    @Min(value = 1, message = "Số lượng trừ tối thiểu là 1")
    private Integer quantity;
}
