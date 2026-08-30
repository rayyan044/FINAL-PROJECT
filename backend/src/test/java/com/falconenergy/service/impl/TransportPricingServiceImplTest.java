package com.falconenergy.service.impl;

import com.falconenergy.dto.TransportDistanceRateRequest;
import com.falconenergy.entity.TransportDistanceRate;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.repository.TransportDistanceRateRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class TransportPricingServiceImplTest {
    @Mock TransportDistanceRateRepository repository;
    @InjectMocks TransportPricingServiceImpl service;

    private TransportDistanceRate rate(String minimum, String maximum, String price) {
        return TransportDistanceRate.builder().minimumKm(new BigDecimal(minimum))
                .maximumKm(maximum == null ? null : new BigDecimal(maximum))
                .price(new BigDecimal(price)).active(true).build();
    }

    @Test void eightKmMatchesZeroToTenBracket() {
        when(repository.matches(new BigDecimal("8"))).thenReturn(List.of(rate("0", "10", "30000")));
        assertEquals(new BigDecimal("30000"), service.resolveDistancePrice(new BigDecimal("8")));
    }

    @Test void twentyFiveKmMatchesFiniteBracket() {
        when(repository.matches(new BigDecimal("25"))).thenReturn(List.of(rate("10.01", "25", "50000")));
        assertEquals(new BigDecimal("50000"), service.resolveDistancePrice(new BigDecimal("25")));
    }

    @Test void eightySixPointSixTwentyFourKmMatchesOpenEndedBracket() {
        when(repository.matches(new BigDecimal("86.624"))).thenReturn(List.of(rate("50", null, "120000")));
        assertEquals(new BigDecimal("120000"), service.resolveDistancePrice(new BigDecimal("86.624")));
    }

    @Test void fiveHundredKmStillMatchesOpenEndedBracket() {
        when(repository.matches(new BigDecimal("500"))).thenReturn(List.of(rate("50", null, "120000")));
        assertEquals(new BigDecimal("120000"), service.resolveDistancePrice(new BigDecimal("500")));
    }

    @Test void missingPricingConfigurationReturnsCustomerFriendlyMessage() {
        when(repository.matches(any())).thenReturn(List.of());
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.resolveDistancePrice(new BigDecimal("99")));
        assertEquals("Transport pricing is currently unavailable for this destination. Please contact Falcon support.", exception.getMessage());
    }

    @Test void overlappingOpenEndedRuleIsRejected() {
        when(repository.overlapsActive(any(), isNull(), isNull())).thenReturn(true);
        TransportDistanceRateRequest request = new TransportDistanceRateRequest(new BigDecimal("70"), null, new BigDecimal("120000"), true);
        assertThrows(BadRequestException.class, () -> service.create(request));
    }

    @Test void secondActiveOpenEndedRuleIsRejected() {
        when(repository.overlapsActive(any(), isNull(), isNull())).thenReturn(true);
        TransportDistanceRateRequest request = new TransportDistanceRateRequest(new BigDecimal("50"), null, new BigDecimal("120000"), true);
        assertThrows(BadRequestException.class, () -> service.create(request));
    }
}
