package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.LoadingActivityWorkflowRequest;
import com.falconenergy.dto.LoadingActivityWorkflowResponse;
import com.falconenergy.service.LoadingActivityWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/loading-activities", "/api/loading-activities"})
@RequiredArgsConstructor
public class LoadingActivityController {
    private final LoadingActivityWorkflowService workflowService;

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingActivityWorkflowResponse>> start(
            @PathVariable Long id,
            @RequestBody(required = false) LoadingActivityWorkflowRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Loading started", workflowService.start(id, request)));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATIONS', 'ROLE_OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingActivityWorkflowResponse>> complete(
            @PathVariable Long id,
            @RequestBody(required = false) LoadingActivityWorkflowRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Loading completed", workflowService.complete(id, request)));
    }
}
