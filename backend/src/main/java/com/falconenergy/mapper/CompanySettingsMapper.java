package com.falconenergy.mapper;

import com.falconenergy.dto.CompanySettingsRequest;
import com.falconenergy.dto.CompanySettingsResponse;
import com.falconenergy.entity.CompanySettings;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CompanySettingsMapper {
    CompanySettingsResponse toResponse(CompanySettings entity);
    CompanySettings toEntity(CompanySettingsRequest request);
    void updateEntityFromRequest(CompanySettingsRequest request, @MappingTarget CompanySettings entity);
}
