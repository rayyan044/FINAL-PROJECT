package com.falconenergy.controller;

import com.falconenergy.dto.AdminDashboardResponse;
import com.falconenergy.dto.ApiResponse;
import com.falconenergy.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/dashboard", "/api/dashboard"})
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final AdminDashboardService adminDashboardService;

    public DashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboardStats() {
        AdminDashboardResponse response = adminDashboardService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard statistics retrieved successfully", response));
    }
}
