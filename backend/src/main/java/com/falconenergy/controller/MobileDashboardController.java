package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.MobileDashboardResponse;
import com.falconenergy.service.MobileDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/mobile", "/api/mobile"})
public class MobileDashboardController {

    private final MobileDashboardService mobileDashboardService;

    public MobileDashboardController(MobileDashboardService mobileDashboardService) {
        this.mobileDashboardService = mobileDashboardService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<MobileDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(
                "Driver mobile dashboard retrieved successfully", mobileDashboardService.getDashboard()));
    }

    @GetMapping("/deliveries")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<MobileDashboardResponse.RecentDelivery>>> getDeliveries() {
        return ResponseEntity.ok(ApiResponse.success(
                "Driver mobile deliveries list retrieved successfully", mobileDashboardService.getDeliveries()));
    }

    @GetMapping("/deliveries/{deliveryId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<MobileDashboardResponse.RecentDelivery>> getDelivery(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Driver mobile delivery retrieved successfully", mobileDashboardService.getDelivery(deliveryId)));
    }
}
