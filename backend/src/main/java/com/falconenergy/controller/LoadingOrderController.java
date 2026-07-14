package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.LoadingOrderRequest;
import com.falconenergy.dto.LoadingOrderResponse;
import com.falconenergy.service.LoadingOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/loading-orders", "/api/loading-orders"})
@RequiredArgsConstructor
public class LoadingOrderController {

    private final LoadingOrderService loadingOrderService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingOrderResponse>> createLoadingOrder(@Valid @RequestBody LoadingOrderRequest request) {
        LoadingOrderResponse response = loadingOrderService.createLoadingOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Loading order created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingOrderResponse>> updateLoadingOrder(
            @PathVariable Long id,
            @Valid @RequestBody LoadingOrderRequest request
    ) {
        LoadingOrderResponse response = loadingOrderService.updateLoadingOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success("Loading order updated successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_SALES_OFFICER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingOrderResponse>> getLoadingOrderById(@PathVariable Long id) {
        LoadingOrderResponse response = loadingOrderService.getLoadingOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Loading order retrieved successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_SALES_OFFICER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingOrderResponse>> getLoadingOrderByOrderId(@PathVariable Long orderId) {
        LoadingOrderResponse response = loadingOrderService.getLoadingOrderByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success("Loading order retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_SALES_OFFICER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<List<LoadingOrderResponse>>> getAllLoadingOrders() {
        List<LoadingOrderResponse> response = loadingOrderService.getAllLoadingOrders();
        return ResponseEntity.ok(ApiResponse.success("All loading orders retrieved successfully", response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingOrderResponse>> approveLoadingOrder(@PathVariable Long id) {
        LoadingOrderResponse response = loadingOrderService.approveLoadingOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Loading order approved and locked successfully", response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingOrderResponse>> cancelLoadingOrder(@PathVariable Long id) {
        LoadingOrderResponse response = loadingOrderService.cancelLoadingOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Loading order cancelled successfully", response));
    }

    @PostMapping("/{id}/activities/{activityId}/start")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingOrderResponse>> startLoadingActivity(
            @PathVariable Long id,
            @PathVariable Long activityId,
            @RequestParam(required = false) String bayNumber,
            @RequestParam(required = false) String pumpNumber
    ) {
        LoadingOrderResponse response = loadingOrderService.startLoadingActivity(id, activityId, bayNumber, pumpNumber);
        return ResponseEntity.ok(ApiResponse.success("Loading activity started successfully", response));
    }

    @PostMapping("/{id}/activities/{activityId}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingOrderResponse>> completeLoadingActivity(
            @PathVariable Long id,
            @PathVariable Long activityId
    ) {
        LoadingOrderResponse response = loadingOrderService.completeLoadingActivity(id, activityId);
        return ResponseEntity.ok(ApiResponse.success("Loading activity completed successfully", response));
    }
}
