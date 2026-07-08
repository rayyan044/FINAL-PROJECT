package com.falconenergy.mapper;

import com.falconenergy.dto.StorageTankRequest;
import com.falconenergy.dto.StorageTankResponse;
import com.falconenergy.entity.StorageTank;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {FuelProductMapper.class})
public interface StorageTankMapper {
    StorageTankResponse toResponse(StorageTank storageTank);

    @Mapping(target = "fuelProduct", ignore = true)
    StorageTank toEntity(StorageTankRequest request);

    @Mapping(target = "fuelProduct", ignore = true)
    void updateEntityFromRequest(StorageTankRequest request, @MappingTarget StorageTank storageTank);
}
