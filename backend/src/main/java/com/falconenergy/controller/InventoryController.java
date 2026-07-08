package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.InventoryRequest;
import com.falconenergy.dto.InventoryResponse;
import com.falconenergy.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping({"/api/v1/inventory", "/api/inventory"})
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<InventoryResponse>> createOrUpdateInventory(
            @Valid @RequestBody InventoryRequest request
    ) {
        InventoryResponse response = inventoryService.createOrUpdateInventory(request);
        return ResponseEntity.ok(ApiResponse.success("Inventory logged successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS', 'FINANCE', 'SALES_OFFICER', 'VIEWER')")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryById(@PathVariable Long id) {
        InventoryResponse response = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory record retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS', 'FINANCE', 'SALES_OFFICER', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getAllInventory(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recordDate,desc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && "asc".equalsIgnoreCase(sort[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<InventoryResponse> records = inventoryService.getAllInventoryRecords(productId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Inventory stock sheets retrieved successfully", records));
    }
}
