package com.falconenergy.controller;

import com.falconenergy.dto.*;
import com.falconenergy.service.FuelPriceRangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/fuel-price-ranges", "/api/fuel-price-ranges"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('FINANCE')")
public class FuelPriceRangeController {
    private final FuelPriceRangeService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FuelPriceRangeResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Fuel price ranges retrieved", service.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FuelPriceRangeResponse>> create(@Valid @RequestBody FuelPriceRangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fuel price range created", service.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FuelPriceRangeResponse>> update(@PathVariable Long id, @Valid @RequestBody FuelPriceRangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fuel price range updated", service.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FuelPriceRangeResponse>> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Fuel price range status updated", service.toggleStatus(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Fuel price range deleted"));
    }
}
