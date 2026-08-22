package com.falconenergy.mapper;

import com.falconenergy.dto.InvoiceResponse;
import com.falconenergy.entity.Invoice;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {FuelOrderMapper.class}, unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface InvoiceMapper {
    InvoiceResponse toResponse(Invoice invoice);
}
