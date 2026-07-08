package com.falconenergy.mapper;

import com.falconenergy.dto.CustomerRequest;
import com.falconenergy.dto.CustomerResponse;
import com.falconenergy.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerResponse toResponse(Customer customer);
    Customer toEntity(CustomerRequest request);
    void updateEntityFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
