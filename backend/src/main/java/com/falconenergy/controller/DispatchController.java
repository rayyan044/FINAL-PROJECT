package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.DispatchResponse;
import com.falconenergy.dto.DispatchRequest;
import com.falconenergy.dto.LoadingActivityResponse;
import com.falconenergy.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/dispatch", "/api/dispatch"})
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_DISPATCHER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<List<LoadingActivityResponse>>> getPendingDispatchActivities() {
        List<LoadingActivityResponse> response = dispatchService.getPendingDispatchActivities();
        return ResponseEntity.ok(ApiResponse.success("Pending dispatch activities retrieved successfully", response));
    }

    @PostMapping("/create/{loadingActivityId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DispatchResponse>> createDispatch(
            @PathVariable Long loadingActivityId,
            @RequestBody(required = false) DispatchRequest request
    ) {
        DispatchResponse response = dispatchService.createDispatch(loadingActivityId, request);
        return ResponseEntity.ok(ApiResponse.success("Dispatch record created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_DISPATCHER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<DispatchResponse>> getDispatchById(@PathVariable Long id) {
        DispatchResponse response = dispatchService.getDispatchById(id);
        return ResponseEntity.ok(ApiResponse.success("Dispatch record retrieved successfully", response));
    }

    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_DISPATCHER', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<DispatchResponse>> getDispatchByActivityId(@PathVariable Long activityId) {
        DispatchResponse response = dispatchService.getDispatchByActivityId(activityId);
        return ResponseEntity.ok(ApiResponse.success("Dispatch record retrieved successfully", response));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DispatchResponse>> releaseTruck(@PathVariable Long id) {
        DispatchResponse response = dispatchService.releaseTruck(id);
        return ResponseEntity.ok(ApiResponse.success("Truck released successfully", response));
    }

    @PostMapping("/{id}/start-transit")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<DispatchResponse>> startTransit(@PathVariable Long id) {
        DispatchResponse response = dispatchService.startTransit(id);
        return ResponseEntity.ok(ApiResponse.success("Transit started successfully", response));
    }
}
