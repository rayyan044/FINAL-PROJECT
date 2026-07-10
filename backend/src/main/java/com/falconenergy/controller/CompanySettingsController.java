package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.CompanySettingsRequest;
import com.falconenergy.dto.CompanySettingsResponse;
import com.falconenergy.service.CompanySettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/company-settings", "/api/company-settings"})
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;

    public CompanySettingsController(CompanySettingsService companySettingsService) {
        this.companySettingsService = companySettingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'SALES_OFFICER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<CompanySettingsResponse>> getCompanySettings() {
        CompanySettingsResponse settings = companySettingsService.getCompanySettings();
        return ResponseEntity.ok(ApiResponse.success("Company settings retrieved successfully", settings));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<ApiResponse<CompanySettingsResponse>> updateCompanySettings(@RequestBody CompanySettingsRequest request) {
        CompanySettingsResponse settings = companySettingsService.updateCompanySettings(request);
        return ResponseEntity.ok(ApiResponse.success("Company settings updated successfully", settings));
    }
}
