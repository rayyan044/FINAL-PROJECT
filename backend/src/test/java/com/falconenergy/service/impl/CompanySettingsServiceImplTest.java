package com.falconenergy.service.impl;

import com.falconenergy.dto.CompanySettingsRequest;
import com.falconenergy.exception.BadRequestException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompanySettingsServiceImplTest {
    private final CompanySettingsServiceImpl service = new CompanySettingsServiceImpl(null, null, null);

    @Test
    void acceptsACompleteDepotLocationSelectedFromTheMap() {
        CompanySettingsRequest request = CompanySettingsRequest.builder()
                .depotName("Falcon Energy Depot").depotAddress("Dar es Salaam, Tanzania")
                .depotLatitude(new BigDecimal("-6.8234567"))
                .depotLongitude(new BigDecimal("39.2698765")).build();
        assertDoesNotThrow(() -> service.validateDepotLocation(request));
    }

    @Test
    void rejectsPartialOrOutOfRangeDepotCoordinates() {
        assertThrows(BadRequestException.class, () -> service.validateDepotLocation(CompanySettingsRequest.builder()
                .depotName("Falcon Depot").depotLatitude(BigDecimal.ZERO).build()));
        assertThrows(BadRequestException.class, () -> service.validateDepotLocation(CompanySettingsRequest.builder()
                .depotName("Falcon Depot").depotAddress("Address")
                .depotLatitude(new BigDecimal("91")).depotLongitude(BigDecimal.ZERO).build()));
    }
}
