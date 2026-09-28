package com.example.promotionservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePromotionRequest {

    @NotNull(message = "Product ID không được để trống")
    @Positive(message = "Product ID phải lớn hơn 0")
    private Long productId;

    @NotBlank(message = "Tên chương trình khuyến mãi không được để trống")
    @Size(max = 255, message = "Tên chương trình không vượt quá 255 ký tự")
    private String promotionName;

    @NotNull(message = "Phần trăm giảm giá không được để trống")
    @Min(value = 1, message = "Phần trăm giảm giá tối thiểu là 1%")
    @Max(value = 100, message = "Phần trăm giảm giá tối đa là 100%")
    private Integer discountPercent;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private Instant startDate;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private Instant endDate;
}
