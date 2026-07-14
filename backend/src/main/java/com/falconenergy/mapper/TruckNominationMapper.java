package com.falconenergy.mapper;

import com.falconenergy.dto.TruckNominationItemDto;
import com.falconenergy.dto.TruckNominationResponse;
import com.falconenergy.dto.TruckNominationRequest;
import com.falconenergy.entity.TruckNomination;
import com.falconenergy.entity.TruckNominationItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TruckNominationMapper {

    @Mapping(target = "customerName", source = "order.customer.companyName")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "orderId", source = "order.id")
    TruckNominationResponse toResponse(TruckNomination entity);

    TruckNominationItemDto toItemDto(TruckNominationItem entity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "items", ignore = true)
    TruckNomination toEntity(TruckNominationRequest request);

    @Mapping(target = "nomination", ignore = true)
    TruckNominationItem toItemEntity(TruckNominationItemDto dto);
}
