package com.falconenergy.mapper;

import com.falconenergy.dto.VehicleRequest;
import com.falconenergy.dto.VehicleResponse;
import com.falconenergy.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {DriverMapper.class})
public interface VehicleMapper {
    VehicleResponse toResponse(Vehicle vehicle);

    @Mapping(target = "driver", ignore = true)
    Vehicle toEntity(VehicleRequest request);

    @Mapping(target = "driver", ignore = true)
    void updateEntityFromRequest(VehicleRequest request, @MappingTarget Vehicle vehicle);
}
