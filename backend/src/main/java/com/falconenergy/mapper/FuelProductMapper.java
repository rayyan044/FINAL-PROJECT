package com.falconenergy.mapper;

import com.falconenergy.dto.FuelProductRequest;
import com.falconenergy.dto.FuelProductResponse;
import com.falconenergy.entity.FuelProduct;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FuelProductMapper {
    FuelProductResponse toResponse(FuelProduct fuelProduct);
    FuelProduct toEntity(FuelProductRequest request);
    void updateEntityFromRequest(FuelProductRequest request, @MappingTarget FuelProduct fuelProduct);
}
