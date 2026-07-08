package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.FuelOrderRequest;
import com.falconenergy.dto.FuelOrderResponse;
import com.falconenergy.service.FuelOrderService;
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
@RequestMapping({"/api/v1/orders", "/api/orders"})
public class FuelOrderController {

    private final FuelOrderService fuelOrderService;

    public FuelOrderController(FuelOrderService fuelOrderService) {
        this.fuelOrderService = fuelOrderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FuelOrderResponse>> createOrder(@Valid @RequestBody FuelOrderRequest request) {
        FuelOrderResponse response = fuelOrderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Fuel order created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FuelOrderResponse>> getOrderById(@PathVariable Long id) {
        FuelOrderResponse response = fuelOrderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Fuel order retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<FuelOrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody FuelOrderRequest request
    ) {
        FuelOrderResponse response = fuelOrderService.updateOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success("Fuel order updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS', 'SALES_OFFICER')")
    public ResponseEntity<ApiResponse<FuelOrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        FuelOrderResponse response = fuelOrderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Fuel order status updated successfully", response));
    }

    @PostMapping("/{id}/approve-edited")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'OPERATIONS', 'SALES_OFFICER')")
    public ResponseEntity<ApiResponse<FuelOrderResponse>> approveOrderWithEdit(
            @PathVariable Long id,
            @Valid @RequestBody com.falconenergy.dto.FuelOrderEditApprovalRequest request
    ) {
        FuelOrderResponse response = fuelOrderService.approveOrderWithEdit(id, request);
        return ResponseEntity.ok(ApiResponse.success("Fuel order approved with edited quantity successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        fuelOrderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Fuel order deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FuelOrderResponse>>> getAllOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && "desc".equalsIgnoreCase(sort[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<FuelOrderResponse> orders = fuelOrderService.getAllOrders(search, status, customerId, productId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Fuel orders retrieved successfully", orders));
    }
}
