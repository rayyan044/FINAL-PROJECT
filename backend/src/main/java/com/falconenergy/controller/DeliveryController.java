package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.DeliveryResponse;
import com.falconenergy.dto.DeliveryArrivalRequest;
import com.falconenergy.dto.DeliveryCompleteRequest;
import com.falconenergy.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/deliveries", "/api/deliveries"})
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_OPERATOR', 'ROLE_OPERATIONS', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getActiveDeliveries() {
        List<DeliveryResponse> response = deliveryService.getActiveDeliveries();
        return ResponseEntity.ok(ApiResponse.success("Active deliveries retrieved successfully", response));
    }

    @PostMapping("/create/{dispatchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_OPERATIONS', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> createDelivery(@PathVariable Long dispatchId) {
        DeliveryResponse response = deliveryService.createDelivery(dispatchId);
        return ResponseEntity.ok(ApiResponse.success("Delivery created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_OPERATOR', 'ROLE_OPERATIONS', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> getDeliveryById(@PathVariable Long id) {
        DeliveryResponse response = deliveryService.getDeliveryById(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery retrieved successfully", response));
    }

    @PostMapping("/{id}/arrival")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_OPERATIONS', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> markArrived(
            @PathVariable Long id,
            @RequestBody(required = false) DeliveryArrivalRequest request
    ) {
        DeliveryResponse response = deliveryService.markArrived(id, request);
        return ResponseEntity.ok(ApiResponse.success("Arrival recorded successfully", response));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR', 'ROLE_OPERATIONS', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> completeDelivery(
            @PathVariable Long id,
            @RequestBody(required = false) DeliveryCompleteRequest request
    ) {
        DeliveryResponse response = deliveryService.completeDelivery(id, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery completed successfully", response));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_OPERATOR', 'ROLE_OPERATIONS', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getDeliveryHistory() {
        List<DeliveryResponse> response = deliveryService.getDeliveryHistory();
        return ResponseEntity.ok(ApiResponse.success("Delivery history retrieved successfully", response));
    }
}
