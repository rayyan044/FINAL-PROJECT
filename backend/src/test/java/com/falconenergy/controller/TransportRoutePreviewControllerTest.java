package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.RouteResult;
import com.falconenergy.dto.TransportRoutePreviewRequest;
import com.falconenergy.entity.CompanySettings;
import com.falconenergy.repository.CompanySettingsRepository;
import com.falconenergy.service.RoutingService;
import com.falconenergy.service.TransportPricingService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportRoutePreviewControllerTest {
    @Mock
    private CompanySettingsRepository settings;
    @Mock
    private RoutingService routes;
    @Mock
    private TransportPricingService pricing;
    @InjectMocks
    private TransportRoutePreviewController controller;

    @Test
    void returnsAnEstimatedRouteWhenNoRoadPolylineIsAvailable() {
        CompanySettings depot = CompanySettings.builder()
                .companyName("Falcon Energy")
                .depotLatitude(new BigDecimal("-6.1659"))
                .depotLongitude(new BigDecimal("39.2026"))
                .build();
        RouteResult estimate = RouteResult.builder()
                .distanceKm(new BigDecimal("9.500"))
                .durationSeconds(760L)
                .provider("GEODESIC_FALLBACK")
                .routeType("ESTIMATED_STRAIGHT_LINE")
                .build();

        when(settings.findFirstByOrderByIdAsc()).thenReturn(Optional.of(depot));
        when(routes.calculateDrivingRoute(any(), any(), any(), any())).thenReturn(estimate);
        when(pricing.resolveDistancePrice(estimate.getDistanceKm())).thenReturn(new BigDecimal("30000"));

        ResponseEntity<ApiResponse<Map<String, Object>>> response = controller.preview(
                new TransportRoutePreviewRequest(new BigDecimal("-6.235251"), new BigDecimal("39.219876")));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ESTIMATED_STRAIGHT_LINE", response.getBody().getData().get("routeType"));
        assertNull(response.getBody().getData().get("polyline"));
    }
}
