package com.falconenergy.mapper;

import com.falconenergy.dto.FuelOrderRequest;
import com.falconenergy.dto.FuelOrderResponse;
import com.falconenergy.entity.FuelOrder;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class, FuelProductMapper.class}, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface FuelOrderMapper {
    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "invoiceNumber", source = "invoice.invoiceNumber")
    @Mapping(target = "paymentStatus", source = "invoice.paymentStatus")
    FuelOrderResponse toResponse(FuelOrder fuelOrder);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "product", ignore = true)
    FuelOrder toEntity(FuelOrderRequest request);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "product", ignore = true)
    void updateEntityFromRequest(FuelOrderRequest request, @MappingTarget FuelOrder fuelOrder);

    @AfterMapping
    default void updateEmergencyCustomerName(FuelOrder fuelOrder, @MappingTarget FuelOrderResponse response) {
        if (fuelOrder != null && response != null && response.getCustomer() != null) {
            String resolvedName = com.falconenergy.util.BuyerNameResolver.resolveName(fuelOrder);
            if (resolvedName != null) {
                response.getCustomer().setCompanyName(resolvedName);
            }
        }
    }
}
