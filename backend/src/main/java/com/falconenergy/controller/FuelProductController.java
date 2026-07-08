package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.FuelProductRequest;
import com.falconenergy.dto.FuelProductResponse;
import com.falconenergy.service.FuelProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/products", "/api/v1/fuel-products", "/api/products", "/api/fuel-products"})
public class FuelProductController {

    private final FuelProductService fuelProductService;

    public FuelProductController(FuelProductService fuelProductService) {
        this.fuelProductService = fuelProductService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<FuelProductResponse>> createProduct(@Valid @RequestBody FuelProductRequest request) {
        FuelProductResponse response = fuelProductService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("Fuel product created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FuelProductResponse>> getProductById(@PathVariable Long id) {
        FuelProductResponse response = fuelProductService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Fuel product retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<FuelProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody FuelProductRequest request
    ) {
        FuelProductResponse response = fuelProductService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Fuel product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        fuelProductService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Fuel product deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FuelProductResponse>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && "desc".equalsIgnoreCase(sort[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<FuelProductResponse> products = fuelProductService.getAllProducts(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Fuel products list retrieved successfully", products));
    }
}
