package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.DeliveryRequest;
import com.falconenergy.dto.DeliveryResponse;
import com.falconenergy.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping({"/api/v1/deliveries", "/api/deliveries"})
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_OPERATOR', 'ROLE_OPERATIONS', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> createDelivery(@Valid @RequestBody DeliveryRequest request) {
        DeliveryResponse response = deliveryService.createDelivery(request);
        return ResponseEntity.ok(ApiResponse.success("Delivery created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> getDeliveryById(@PathVariable Long id) {
        DeliveryResponse response = deliveryService.getDeliveryById(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_OPERATOR', 'ROLE_OPERATIONS', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> updateDelivery(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryRequest request
    ) {
        DeliveryResponse response = deliveryService.updateDelivery(id, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_OPERATOR', 'ROLE_OPERATIONS', 'ROLE_DRIVER', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> updateDeliveryStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        DeliveryResponse response = deliveryService.updateDeliveryStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Delivery status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteDelivery(@PathVariable Long id) {
        deliveryService.deleteDelivery(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeliveryResponse>>> getAllDeliveries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && "desc".equalsIgnoreCase(sort[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<DeliveryResponse> deliveries = deliveryService.getAllDeliveries(search, status, driverId, vehicleId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Deliveries retrieved successfully", deliveries));
    }
}
