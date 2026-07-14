package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.TruckNominationRequest;
import com.falconenergy.dto.TruckNominationResponse;
import com.falconenergy.service.TruckNominationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/truck-nominations", "/api/truck-nominations"})
@RequiredArgsConstructor
public class TruckNominationController {

    private final TruckNominationService truckNominationService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SALES_OFFICER')")
    public ResponseEntity<ApiResponse<TruckNominationResponse>> createNomination(@Valid @RequestBody TruckNominationRequest request) {
        TruckNominationResponse response = truckNominationService.createNominationDraft(request);
        return ResponseEntity.ok(ApiResponse.success("Truck nomination draft created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SALES_OFFICER')")
    public ResponseEntity<ApiResponse<TruckNominationResponse>> updateNomination(
            @PathVariable Long id,
            @Valid @RequestBody TruckNominationRequest request
    ) {
        TruckNominationResponse response = truckNominationService.updateNominationDraft(id, request);
        return ResponseEntity.ok(ApiResponse.success("Truck nomination updated successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_SALES_OFFICER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<TruckNominationResponse>> getNominationById(@PathVariable Long id) {
        TruckNominationResponse response = truckNominationService.getNominationById(id);
        return ResponseEntity.ok(ApiResponse.success("Truck nomination retrieved successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_SALES_OFFICER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<TruckNominationResponse>> getNominationByOrderId(@PathVariable Long orderId) {
        TruckNominationResponse response = truckNominationService.getNominationByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success("Truck nomination retrieved successfully", response));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('ROLE_SALES_OFFICER')")
    public ResponseEntity<ApiResponse<TruckNominationResponse>> submitNomination(@PathVariable Long id) {
        TruckNominationResponse response = truckNominationService.submitNomination(id);
        return ResponseEntity.ok(ApiResponse.success("Truck nomination submitted successfully. Order is now Ready for Loading.", response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<TruckNominationResponse>> approveNomination(@PathVariable Long id) {
        TruckNominationResponse response = truckNominationService.approveNomination(id);
        return ResponseEntity.ok(ApiResponse.success("Truck nomination approved successfully", response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SALES_OFFICER')")
    public ResponseEntity<ApiResponse<TruckNominationResponse>> cancelNomination(@PathVariable Long id) {
        TruckNominationResponse response = truckNominationService.cancelNomination(id);
        return ResponseEntity.ok(ApiResponse.success("Truck nomination cancelled successfully", response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> requestChanges(
            @PathVariable Long id,
            @RequestParam String reason
    ) {
        truckNominationService.requestChanges(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Changes requested successfully. Truck nomination has been returned to DRAFT."));
    }
}
