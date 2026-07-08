package com.falconenergy.mapper;

import com.falconenergy.dto.InventoryRequest;
import com.falconenergy.dto.InventoryResponse;
import com.falconenergy.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {FuelProductMapper.class})
public interface InventoryMapper {
    InventoryResponse toResponse(Inventory inventory);

    @Mapping(target = "product", ignore = true)
    Inventory toEntity(InventoryRequest request);

    @Mapping(target = "product", ignore = true)
    void updateEntityFromRequest(InventoryRequest request, @MappingTarget Inventory inventory);
}
