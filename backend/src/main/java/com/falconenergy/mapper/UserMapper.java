package com.falconenergy.mapper;

import com.falconenergy.dto.UserRegisterRequest;
import com.falconenergy.dto.UserResponse;
import com.falconenergy.dto.UserUpdateRequest;
import com.falconenergy.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "driver.id", target = "driverId")
    UserResponse toResponse(User user);

    @Mapping(target = "driver", ignore = true)
    User toEntity(UserRegisterRequest request);

    @Mapping(target = "driver", ignore = true)
    void updateEntityFromRequest(UserUpdateRequest request, @MappingTarget User user);
}
