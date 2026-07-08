package com.falconenergy.mapper;

import com.falconenergy.dto.DriverRequest;
import com.falconenergy.dto.DriverResponse;
import com.falconenergy.entity.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DriverMapper {
    DriverResponse toResponse(Driver driver);
    Driver toEntity(DriverRequest request);
    void updateEntityFromRequest(DriverRequest request, @MappingTarget Driver driver);
}
