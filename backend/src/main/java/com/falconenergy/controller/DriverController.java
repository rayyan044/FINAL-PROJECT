package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.DriverRequest;
import com.falconenergy.dto.DriverResponse;
import com.falconenergy.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/drivers", "/api/drivers"})
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(@Valid @RequestBody DriverRequest request) {
        DriverResponse response = driverService.createDriver(request);
        return ResponseEntity.ok(ApiResponse.success("Driver created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(@PathVariable Long id) {
        DriverResponse response = driverService.getDriverById(id);
        return ResponseEntity.ok(ApiResponse.success("Driver retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverRequest request
    ) {
        DriverResponse response = driverService.updateDriver(id, request);
        return ResponseEntity.ok(ApiResponse.success("Driver updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.ok(ApiResponse.success("Driver deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DriverResponse>>> getAllDrivers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && "desc".equalsIgnoreCase(sort[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<DriverResponse> drivers = driverService.getAllDrivers(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Drivers list retrieved successfully", drivers));
    }
}
