package com.example.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest implements Serializable {

    @NotNull(message = "Customer ID không được để trống")
    private Long customerId;

    @NotBlank(message = "Customer Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String customerEmail;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    @NotEmpty(message = "Danh sách sản phẩm không được rỗng")
    @Valid
    private List<OrderItemRequest> items;
}
