package com.falconenergy.mapper;

import com.falconenergy.dto.DeliveryRequest;
import com.falconenergy.dto.DeliveryResponse;
import com.falconenergy.entity.Delivery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {DriverMapper.class, VehicleMapper.class, FuelOrderMapper.class})
public interface DeliveryMapper {
    DeliveryResponse toResponse(Delivery delivery);

    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "order", ignore = true)
    Delivery toEntity(DeliveryRequest request);

    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "order", ignore = true)
    void updateEntityFromRequest(DeliveryRequest request, @MappingTarget Delivery delivery);
}
