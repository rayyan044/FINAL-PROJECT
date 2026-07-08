package com.falconenergy.controller;

import com.falconenergy.dto.*;
import com.falconenergy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping({"/api/v1/users", "/api/users"})
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRegisterRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        UserResponse response = userService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", response));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<UserResponse>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordResetRequest request
    ) {
        UserResponse response = userService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("User password reset successfully", response));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateSelfProfile(
            Principal principal,
            @Valid @RequestBody SelfProfileUpdateRequest request
    ) {
        UserResponse response = userService.updateSelfProfile(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort
    ) {
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && "desc".equalsIgnoreCase(sort[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<UserResponse> users = userService.getAllUsers(search, role, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users list retrieved successfully", users));
    }
}
