package com.falconenergy.service.impl;

import com.falconenergy.dto.UserRegisterRequest;
import com.falconenergy.dto.UserResponse;
import com.falconenergy.dto.UserUpdateRequest;
import com.falconenergy.entity.Role;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.UserMapper;
import com.falconenergy.repository.RoleRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.service.AuditService;
import com.falconenergy.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    public UserResponse createUser(UserRegisterRequest request) {
        log.info("Creating new user via Admin service: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        Role role = roleRepository.findByRoleName(request.getRole().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoleEntity(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordChanged(true);
        user.setFullName(request.getFirstName() + " " + request.getLastName());

        User saved = userRepository.save(user);

        // Audit Log
        auditService.logAction(
                null,
                "USER_MANAGEMENT",
                "USER_CREATED",
                "USER",
                saved.getId(),
                null,
                "User account created for username: " + saved.getUsername(),
                null
        );

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user via Admin service: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check unique constraints if values changed
        if (request.getUsername() != null && !request.getUsername().equalsIgnoreCase(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException("Username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        String oldVal = "Username: " + user.getUsername() + ", Email: " + user.getEmail() + ", Role: " + (user.getRoleEntity() != null ? user.getRoleEntity().getRoleName() : "None") + ", Status: " + user.getStatus();

        if (request.getRole() != null) {
            Role role = roleRepository.findByRoleName(request.getRole().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));
            user.setRoleEntity(role);
        }

        if (request.getStatus() != null) {
            try {
                user.setStatus(UserStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus());
            }
        }

        userMapper.updateEntityFromRequest(request, user);
        user.setFullName(user.getFirstName() + " " + user.getLastName());

        User saved = userRepository.save(user);

        String newVal = "Username: " + saved.getUsername() + ", Email: " + saved.getEmail() + ", Role: " + (saved.getRoleEntity() != null ? saved.getRoleEntity().getRoleName() : "None") + ", Status: " + saved.getStatus();

        // Audit Log
        auditService.logAction(
                null,
                "USER_MANAGEMENT",
                "USER_UPDATED",
                "USER",
                saved.getId(),
                oldVal,
                newVal,
                null
        );

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse assignRole(Long userId, Long roleId) {
        log.info("Assigning role {} to user {}", roleId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        String oldRole = user.getRoleEntity() != null ? user.getRoleEntity().getRoleName() : "NONE";
        user.setRoleEntity(role);
        User saved = userRepository.save(user);

        // Audit Log
        auditService.logAction(
                null,
                "USER_MANAGEMENT",
                "ROLE_CHANGED",
                "USER",
                saved.getId(),
                "Role: " + oldRole,
                "Role: " + role.getRoleName(),
                null
        );

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse changeUserStatus(Long userId, String status) {
        log.info("Changing status of user {} to {}", userId, status);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String oldStatus = user.getStatus().name();
        try {
            user.setStatus(UserStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }

        User saved = userRepository.save(user);

        // Audit Log
        auditService.logAction(
                null,
                "USER_MANAGEMENT",
                "USER_UPDATED",
                "USER",
                saved.getId(),
                "Status: " + oldStatus,
                "Status: " + saved.getStatus().name(),
                null
        );

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse resetPassword(Long userId, String password) {
        log.info("Resetting password for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordChanged(false); // force password change on next login if needed

        User saved = userRepository.save(user);

        // Audit Log
        auditService.logAction(
                null,
                "USER_MANAGEMENT",
                "USER_UPDATED",
                "USER",
                saved.getId(),
                "Password changed: false",
                "Password reset by Admin",
                null
        );

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
