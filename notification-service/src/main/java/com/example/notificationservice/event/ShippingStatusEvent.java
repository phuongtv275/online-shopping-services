package com.example.notificationservice.event;

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
public class ShippingStatusEvent implements Serializable {
    private Long shippingId;
    private Long orderId;
    private String trackingNumber;
    private String status;
    private String shipperName;
    private String shipperPhone;
    private String shippingAddress;
    private String note;
    private Instant deliveredAt;
    private String correlationId;
    private Instant timestamp;
}
