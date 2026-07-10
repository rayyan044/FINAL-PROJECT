package com.falconenergy.mapper;

import com.falconenergy.dto.PaymentAccountRequest;
import com.falconenergy.dto.PaymentAccountResponse;
import com.falconenergy.entity.PaymentAccount;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentAccountMapper {
    PaymentAccountResponse toResponse(PaymentAccount entity);
    PaymentAccount toEntity(PaymentAccountRequest request);
    void updateEntityFromRequest(PaymentAccountRequest request, @MappingTarget PaymentAccount entity);
}
