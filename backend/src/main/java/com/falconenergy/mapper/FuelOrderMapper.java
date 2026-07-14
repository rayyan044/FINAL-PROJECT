package com.falconenergy.mapper;

import com.falconenergy.dto.FuelOrderRequest;
import com.falconenergy.dto.FuelOrderResponse;
import com.falconenergy.entity.FuelOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class, FuelProductMapper.class})
public interface FuelOrderMapper {
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "invoiceNumber", source = "invoice.invoiceNumber")
    @Mapping(target = "paymentStatus", source = "invoice.paymentStatus")
    @Mapping(target = "truckNominationStatus", source = "truckNomination.status")
    FuelOrderResponse toResponse(FuelOrder fuelOrder);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "product", ignore = true)
    FuelOrder toEntity(FuelOrderRequest request);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "product", ignore = true)
    void updateEntityFromRequest(FuelOrderRequest request, @MappingTarget FuelOrder fuelOrder);
}
