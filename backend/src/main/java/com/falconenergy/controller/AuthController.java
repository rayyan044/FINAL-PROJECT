package com.falconenergy.controller;

import com.falconenergy.dto.ApiResponse;
import com.falconenergy.dto.RefreshTokenRequest;
import com.falconenergy.dto.TokenResponse;
import com.falconenergy.dto.UserRegisterRequest;
import com.falconenergy.dto.UserLoginRequest;
import com.falconenergy.dto.UserResponse;
import com.falconenergy.dto.CustomerRegistrationRequest;
import com.falconenergy.dto.SelfProfileUpdateRequest;
import com.falconenergy.service.UserService;
import com.falconenergy.service.CustomerRegistrationService;
import com.falconenergy.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class AuthController {

    private final UserService userService;
    private final CustomerRegistrationService customerRegistrationService;

    public AuthController(UserService userService, CustomerRegistrationService customerRegistrationService) {
        this.userService = userService;
        this.customerRegistrationService = customerRegistrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        // This endpoint used to accept a caller-selected role.  Keeping it public
        // would let an internet caller attempt privileged account creation.
        throw new BadRequestException("Use the customer registration endpoint. Staff accounts are created by an administrator.");
    }

    @PostMapping("/customer-registration")
    public ResponseEntity<ApiResponse<UserResponse>> registerCustomer(@Valid @RequestBody CustomerRegistrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer account registered successfully", customerRegistrationService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody UserLoginRequest request) {
        TokenResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = userService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            userService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", userService.getSelfProfile(principal.getName())));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(
            Principal principal,
            @Valid @RequestBody SelfProfileUpdateRequest request
    ) {
        UserResponse response = userService.updateSelfProfile(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", response));
    }
}
