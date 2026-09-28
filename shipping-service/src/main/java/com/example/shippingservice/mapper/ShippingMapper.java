package com.example.shippingservice.mapper;

import com.example.shippingservice.dto.CreateShippingRequest;
import com.example.shippingservice.dto.ShippingResponse;
import com.example.shippingservice.entity.Shipping;
import com.example.shippingservice.event.ShippingStatusEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShippingMapper {

    ShippingResponse toDto(Shipping shipping);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "trackingNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Shipping toEntity(CreateShippingRequest request);

    @Mapping(target = "shippingId", source = "id")
    @Mapping(target = "correlationId", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    ShippingStatusEvent toEvent(Shipping shipping);
}
