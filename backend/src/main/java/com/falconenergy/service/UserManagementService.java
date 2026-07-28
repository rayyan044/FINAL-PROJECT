package com.falconenergy.service;

import com.falconenergy.dto.UserRegisterRequest;
import com.falconenergy.dto.UserResponse;
import com.falconenergy.dto.UserUpdateRequest;
import java.util.List;

public interface UserManagementService {
    UserResponse createUser(UserRegisterRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    UserResponse assignRole(Long userId, Long roleId);
    UserResponse changeUserStatus(Long userId, String status);
    UserResponse resetPassword(Long userId, String password);
    List<UserResponse> getUsers();
    UserResponse getUserById(Long id);
}
