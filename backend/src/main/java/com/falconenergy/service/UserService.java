package com.falconenergy.service;

import com.falconenergy.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    TokenResponse login(UserLoginRequest request);
    UserResponse register(UserRegisterRequest request);
    TokenResponse refreshToken(RefreshTokenRequest request);
    void logout(String token);
    UserResponse createUser(UserRegisterRequest request);
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    UserResponse updateStatus(Long id, StatusUpdateRequest request);
    UserResponse resetPassword(Long id, PasswordResetRequest request);
    UserResponse updateSelfProfile(String currentUserEmailOrUsername, SelfProfileUpdateRequest request);
    void deleteUser(Long id);
    Page<UserResponse> getAllUsers(String search, String role, String status, Pageable pageable);
}
