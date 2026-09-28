package com.example.shippingservice.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliverShippingRequest {

    @Size(max = 500, message = "Ghi chú khi giao hàng không vượt quá 500 ký tự")
    private String deliveryNote;
}
