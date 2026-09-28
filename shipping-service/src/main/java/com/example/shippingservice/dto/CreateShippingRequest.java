package com.example.shippingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShippingRequest {

    @NotNull(message = "Mã đơn hàng (orderId) không được để trống")
    @Positive(message = "Mã đơn hàng phải lớn hơn 0")
    private Long orderId;

    @NotBlank(message = "Tên shipper không được để trống")
    @Size(max = 255, message = "Tên shipper không được vượt quá 255 ký tự")
    private String shipperName;

    @NotBlank(message = "Số điện thoại shipper không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại shipper không hợp lệ theo định dạng Việt Nam")
    private String shipperPhone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    @Size(max = 500, message = "Ghi chú giao hàng không được vượt quá 500 ký tự")
    private String note;
}
