package com.example.inventoryservice.mapper;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    InventoryResponse toDto(Inventory entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservedQuantity", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Inventory toEntity(InventoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "reservedQuantity", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(InventoryRequest request, @MappingTarget Inventory entity);
}
