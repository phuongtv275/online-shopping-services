package com.example.orderservice.mapper;

import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.event.OrderItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toDto(Order order);

    OrderItemResponse toDto(OrderItem item);

    OrderItemDto toEventItemDto(OrderItem item);

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "correlationId", ignore = true)
    OrderCreatedEvent toEvent(Order order);
}
