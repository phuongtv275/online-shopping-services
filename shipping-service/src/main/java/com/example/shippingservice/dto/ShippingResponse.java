package com.example.shippingservice.dto;

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
public class ShippingResponse implements Serializable {

    private Long id;
    private Long orderId;
    private String trackingNumber;
    private String shipperName;
    private String shipperPhone;
    private String shippingAddress;
    private String status;
    private String note;
    private Instant deliveredAt;
    private Instant createdAt;
    private Instant updatedAt;
}
