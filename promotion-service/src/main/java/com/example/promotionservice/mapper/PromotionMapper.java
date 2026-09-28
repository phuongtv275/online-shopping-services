package com.example.promotionservice.mapper;

import com.example.promotionservice.dto.CreatePromotionRequest;
import com.example.promotionservice.dto.PromotionResponse;
import com.example.promotionservice.entity.Promotion;
import com.example.promotionservice.event.PromotionUpdatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromotionMapper {

    PromotionResponse toDto(Promotion promotion);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Promotion toEntity(CreatePromotionRequest request);

    @Mapping(target = "correlationId", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    PromotionUpdatedEvent toEvent(Promotion promotion);
}
