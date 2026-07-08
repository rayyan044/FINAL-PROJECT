package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.StorageTankRequest;
import com.falconenergy.dto.StorageTankResponse;
import com.falconenergy.service.StorageTankService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping({"/api/v1/tanks", "/api/tanks"})
public class StorageTankController {

    private final StorageTankService storageTankService;

    public StorageTankController(StorageTankService storageTankService) {
        this.storageTankService = storageTankService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<StorageTankResponse>> createTank(@Valid @RequestBody StorageTankRequest request) {
        StorageTankResponse response = storageTankService.createTank(request);
        return ResponseEntity.ok(ApiResponse.success("Storage tank created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS', 'FINANCE', 'SALES_OFFICER', 'VIEWER')")
    public ResponseEntity<ApiResponse<StorageTankResponse>> getTankById(@PathVariable Long id) {
        StorageTankResponse response = storageTankService.getTankById(id);
        return ResponseEntity.ok(ApiResponse.success("Storage tank retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<StorageTankResponse>> updateTank(
            @PathVariable Long id,
            @Valid @RequestBody StorageTankRequest request
    ) {
        StorageTankResponse response = storageTankService.updateTank(id, request);
        return ResponseEntity.ok(ApiResponse.success("Storage tank updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTank(@PathVariable Long id) {
        storageTankService.deleteTank(id);
        return ResponseEntity.ok(ApiResponse.success("Storage tank deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS', 'FINANCE', 'SALES_OFFICER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<StorageTankResponse>>> getAllTanks(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && "desc".equalsIgnoreCase(sort[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<StorageTankResponse> tanks = storageTankService.getAllTanks(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Storage tanks list retrieved successfully", tanks));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<Void>> adjustVolume(
            @PathVariable Long id,
            @RequestParam BigDecimal amount
    ) {
        storageTankService.adjustVolume(id, amount);
        return ResponseEntity.ok(ApiResponse.success("Storage tank volume adjusted successfully"));
    }
}
