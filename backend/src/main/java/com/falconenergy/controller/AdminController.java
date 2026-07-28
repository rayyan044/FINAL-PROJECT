package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.UserRegisterRequest;
import com.falconenergy.dto.UserResponse;
import com.falconenergy.dto.UserUpdateRequest;
import com.falconenergy.entity.AuditLog;
import com.falconenergy.entity.Role;
import com.falconenergy.entity.SystemSetting;
import com.falconenergy.repository.RoleRepository;
import com.falconenergy.service.AuditService;
import com.falconenergy.service.SystemSettingService;
import com.falconenergy.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/v1/admin", "/api/admin"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserManagementService userManagementService;
    private final RoleRepository roleRepository;
    private final AuditService auditService;
    private final SystemSettingService systemSettingService;

    // --- Users Management ---

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("REST request to list all users (admin)");
        List<UserResponse> users = userManagementService.getUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRegisterRequest request) {
        log.info("REST request to create user: {}", request.getUsername());
        UserResponse response = userManagementService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", response));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        log.info("REST request to update user: {}", id);
        UserResponse response = userManagementService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        log.info("REST request to change user {} status to: {}", id, status);
        UserResponse response = userManagementService.changeUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", response));
    }

    // --- Roles Management ---

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        log.info("REST request to list all roles");
        List<Role> roles = roleRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body
    ) {
        Long roleId = body.get("roleId");
        log.info("REST request to assign role {} to user {}", roleId, id);
        UserResponse response = userManagementService.assignRole(id, roleId);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", response));
    }

    // --- Audit Logs ---

    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditHistory() {
        log.info("REST request to get audit history");
        List<AuditLog> logs = auditService.getAuditHistory();
        return ResponseEntity.ok(ApiResponse.success("Audit history retrieved successfully", logs));
    }

    @GetMapping("/audit/user/{username}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getUserActivity(@PathVariable String username) {
        log.info("REST request to get audit history for user: {}", username);
        List<AuditLog> logs = auditService.getUserActivity(username);
        return ResponseEntity.ok(ApiResponse.success("User activity logs retrieved successfully", logs));
    }

    @GetMapping("/audit/entity/{type}/{id}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getEntityHistory(
            @PathVariable String type,
            @PathVariable Long id
    ) {
        log.info("REST request to get audit history for entity {} with id {}", type, id);
        List<AuditLog> logs = auditService.getEntityHistory(type.toUpperCase(), id);
        return ResponseEntity.ok(ApiResponse.success("Entity history logs retrieved successfully", logs));
    }

    // --- System Settings ---

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getAllSettings() {
        log.info("REST request to list system settings");
        List<SystemSetting> settings = systemSettingService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success("System settings retrieved successfully", settings));
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<ApiResponse<SystemSetting>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body
    ) {
        String value = body.get("value");
        log.info("REST request to update setting: {} to {}", key, value);
        SystemSetting setting = systemSettingService.updateSetting(key.toUpperCase(), value);
        return ResponseEntity.ok(ApiResponse.success("System setting updated successfully", setting));
    }
}
