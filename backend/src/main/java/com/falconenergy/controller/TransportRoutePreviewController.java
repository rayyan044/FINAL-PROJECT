package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.RouteResult;
import com.falconenergy.dto.TransportRoutePreviewRequest;
import com.falconenergy.entity.CompanySettings;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.repository.CompanySettingsRepository;
import com.falconenergy.service.RoutingService;
import com.falconenergy.service.TransportPricingService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/transport-route-preview", "/api/transport-route-preview"})
public class TransportRoutePreviewController {
    private final CompanySettingsRepository settings;
    private final RoutingService routes;
    private final TransportPricingService pricing;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> preview(@Valid @RequestBody TransportRoutePreviewRequest request) {
        CompanySettings depot = settings.findFirstByOrderByIdAsc().orElseThrow(() ->
                new BadRequestException("Falcon depot location has not been configured. Please configure the depot location in Company Settings."));
        if (depot.getDepotLatitude() == null || depot.getDepotLongitude() == null) {
            throw new BadRequestException("Falcon depot location has not been configured. Please configure the depot location in Company Settings.");
        }

        RouteResult route = routes.calculateDrivingRoute(
                depot.getDepotLatitude(), depot.getDepotLongitude(), request.getLatitude(), request.getLongitude());
        BigDecimal price = pricing.resolveDistancePrice(route.getDistanceKm());

        // Map.of rejects null values. An estimated route deliberately has no
        // road polyline, so preserve that valid fallback response instead of
        // converting a temporary routing outage into an HTTP 500 error.
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("distanceKm", route.getDistanceKm());
        preview.put("durationSeconds", route.getDurationSeconds());
        preview.put("transportPrice", price);
        preview.put("routeType", route.getRouteType());
        preview.put("provider", route.getProvider());
        preview.put("polyline", route.getPolyline());

        return ResponseEntity.ok(ApiResponse.success("Driving route calculated", preview));
    }
}
