package com.falconenergy.mapper;

import com.falconenergy.dto.FuelTransactionRequest;
import com.falconenergy.dto.FuelTransactionResponse;
import com.falconenergy.entity.FuelTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {FuelProductMapper.class})
public interface FuelTransactionMapper {
    FuelTransactionResponse toResponse(FuelTransaction fuelTransaction);

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "storageTank", ignore = true)
    FuelTransaction toEntity(FuelTransactionRequest request);

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "storageTank", ignore = true)
    void updateEntityFromRequest(FuelTransactionRequest request, @MappingTarget FuelTransaction fuelTransaction);
}
