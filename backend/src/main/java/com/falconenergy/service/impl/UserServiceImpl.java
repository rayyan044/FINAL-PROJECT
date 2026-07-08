package com.falconenergy.service.impl;

import com.falconenergy.dto.*;
import com.falconenergy.entity.Driver;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.UserMapper;
import com.falconenergy.repository.DriverRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.security.CustomUserDetails;
import com.falconenergy.security.JwtTokenProvider;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final DriverRepository driverRepository;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuthenticationManager authenticationManager,
            AuditLogService auditLogService,
            DriverRepository driverRepository
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.auditLogService = auditLogService;
        this.driverRepository = driverRepository;
    }

    @Override
    public TokenResponse login(UserLoginRequest request) {
        log.info("Attempting login for credential: {}", request.getEmail());
        
        // Load user first to authenticate against email or username
        User user = userRepository.findByEmail(request.getEmail())
                .or(() -> userRepository.findByUsername(request.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email or username: " + request.getEmail()));

        // Authenticate with security manager using the loaded UserDetails username (email)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User account is inactive");
        }

        UserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        log.info("Login successful for: {}", user.getEmail());
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .passwordChanged(user.isPasswordChanged())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .driverId(user.getDriver() != null ? user.getDriver().getId() : null)
                .build();
    }

    @Override
    public UserResponse register(UserRegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordChanged(true); // Self registration defaults to true

        try {
            user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role specified: " + request.getRole());
        }

        // Validate and link Driver profile
        if (request.getDriverId() != null) {
            if (user.getRole() != UserRole.DRIVER) {
                throw new BadRequestException("Only users with DRIVER role can be linked to a driver profile");
            }
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));
            userRepository.findByDriverId(request.getDriverId()).ifPresent(existingUser -> {
                throw new BadRequestException("This driver profile is already linked to another user account");
            });
            user.setDriver(driver);
        } else {
            if (user.getRole() == UserRole.DRIVER) {
                throw new BadRequestException("Driver role must be linked to a driver profile");
            }
        }

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String username = jwtTokenProvider.extractUsername(refreshToken);

        if (username != null) {
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            UserDetails userDetails = new CustomUserDetails(user);

            if (jwtTokenProvider.isTokenValid(refreshToken, userDetails)) {
                String accessToken = jwtTokenProvider.generateToken(userDetails);
                return TokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .passwordChanged(user.isPasswordChanged())
                        .phone(user.getPhone())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .driverId(user.getDriver() != null ? user.getDriver().getId() : null)
                        .build();
            }
        }
        throw new BadRequestException("Invalid refresh token");
    }

    @Override
    public void logout(String token) {
        log.info("User logout requested");
    }

    @Override
    public UserResponse createUser(UserRegisterRequest request) {
        log.info("Admin creating user: {}", request.getUsername());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordChanged(false); // Created by admin, require change on first login!

        try {
            user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role specified: " + request.getRole());
        }

        // Validate and link Driver profile
        if (request.getDriverId() != null) {
            if (user.getRole() != UserRole.DRIVER) {
                throw new BadRequestException("Only users with DRIVER role can be linked to a driver profile");
            }
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));
            userRepository.findByDriverId(request.getDriverId()).ifPresent(existingUser -> {
                throw new BadRequestException("This driver profile is already linked to another user account");
            });
            user.setDriver(driver);
        } else {
            if (user.getRole() == UserRole.DRIVER) {
                throw new BadRequestException("Driver role must be linked to a driver profile");
            }
        }

        User savedUser = userRepository.save(user);
        
        // Log audit action
        auditLogService.log(
                "USER_CREATE",
                "USER",
                savedUser.getId(),
                savedUser.getUsername(),
                "User account created by administrator."
        );

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Get currently logged-in user
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUsername)
                .or(() -> userRepository.findByUsername(currentUsername))
                .orElse(null);

        // Check uniqueness of username if changed
        if (request.getUsername() != null && !request.getUsername().equalsIgnoreCase(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException("Username already exists: " + request.getUsername());
            }
        }

        // Self-protection: demoting or deactivating yourself
        if (currentUser != null && currentUser.getId().equals(id)) {
            UserStatus newStatus = UserStatus.valueOf(request.getStatus().toUpperCase());
            if (newStatus != UserStatus.ACTIVE) {
                throw new BadRequestException("An administrator cannot deactivate their own account.");
            }
            UserRole newRole = UserRole.valueOf(request.getRole().toUpperCase());
            if (newRole != UserRole.ADMIN) {
                throw new BadRequestException("An administrator cannot demote their own account.");
            }
        }

        // Self-protection: deactivating or demoting the last active administrator
        if (user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.ACTIVE) {
            UserStatus newStatus = UserStatus.valueOf(request.getStatus().toUpperCase());
            UserRole newRole = UserRole.valueOf(request.getRole().toUpperCase());
            if (newStatus != UserStatus.ACTIVE || newRole != UserRole.ADMIN) {
                long activeAdmins = userRepository.findAll().stream()
                        .filter(u -> u.getRole() == UserRole.ADMIN && u.getStatus() == UserStatus.ACTIVE && !u.getId().equals(id))
                        .count();
                if (activeAdmins == 0) {
                    throw new BadRequestException("Cannot demote or deactivate this administrator account. At least one active administrator must remain in the system.");
                }
            }
        }

        userMapper.updateEntityFromRequest(request, user);

        try {
            user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role specified: " + request.getRole());
        }

        try {
            user.setStatus(UserStatus.valueOf(request.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status specified: " + request.getStatus());
        }

        // Validate and link Driver profile
        if (request.getDriverId() != null) {
            if (user.getRole() != UserRole.DRIVER) {
                throw new BadRequestException("Only users with DRIVER role can be linked to a driver profile");
            }
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));
            userRepository.findByDriverId(request.getDriverId()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new BadRequestException("This driver profile is already linked to another user account");
                }
            });
            user.setDriver(driver);
        } else {
            if (user.getRole() == UserRole.DRIVER) {
                throw new BadRequestException("Driver role must be linked to a driver profile");
            }
            user.setDriver(null);
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getEmail());

        // Log audit action
        auditLogService.log(
                "USER_UPDATE",
                "USER",
                updatedUser.getId(),
                updatedUser.getUsername(),
                "User details updated by administrator."
        );

        return userMapper.toResponse(updatedUser);
    }

    @Override
    public UserResponse updateStatus(Long id, StatusUpdateRequest request) {
        log.info("Updating status for user id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Get currently logged-in user
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUsername)
                .or(() -> userRepository.findByUsername(currentUsername))
                .orElse(null);

        UserStatus newStatus;
        try {
            newStatus = UserStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status specified: " + request.getStatus());
        }

        // Self-protection
        if (currentUser != null && currentUser.getId().equals(id)) {
            if (newStatus != UserStatus.ACTIVE) {
                throw new BadRequestException("An administrator cannot deactivate their own account.");
            }
        }

        // Self-protection: last active administrator
        if (user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.ACTIVE && newStatus != UserStatus.ACTIVE) {
            long activeAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.ADMIN && u.getStatus() == UserStatus.ACTIVE && !u.getId().equals(id))
                    .count();
            if (activeAdmins == 0) {
                throw new BadRequestException("Cannot deactivate this administrator account. At least one active administrator must remain in the system.");
            }
        }

        user.setStatus(newStatus);
        User saved = userRepository.save(user);

        // Log audit action
        auditLogService.log(
                "USER_STATUS_CHANGE",
                "USER",
                saved.getId(),
                saved.getUsername(),
                "User account status changed to " + newStatus.name() + "."
        );

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse resetPassword(Long id, PasswordResetRequest request) {
        log.info("Resetting password for user id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordChanged(false); // Admin reset password, require change on next login!
        User saved = userRepository.save(user);

        // Log audit action
        auditLogService.log(
                "USER_PASSWORD_RESET",
                "USER",
                saved.getId(),
                saved.getUsername(),
                "User password reset by administrator."
        );

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateSelfProfile(String currentUserEmailOrUsername, SelfProfileUpdateRequest request) {
        log.info("User profile self-update requested: {}", currentUserEmailOrUsername);
        User user = userRepository.findByEmail(currentUserEmailOrUsername)
                .or(() -> userRepository.findByUsername(currentUserEmailOrUsername))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserEmailOrUsername));

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (request.getConfirmPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
                throw new BadRequestException("Passwords do not match");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setPasswordChanged(true); // User set their own password, clear flag
        }

        User saved = userRepository.save(user);

        // Log audit action
        auditLogService.log(
                "USER_SELF_PROFILE_UPDATE",
                "USER",
                saved.getId(),
                saved.getUsername(),
                "User updated their own profile."
        );

        return userMapper.toResponse(saved);
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Soft deleting user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Get currently logged-in user
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUsername)
                .or(() -> userRepository.findByUsername(currentUsername))
                .orElse(null);

        // Self-protection
        if (currentUser != null && currentUser.getId().equals(id)) {
            throw new BadRequestException("An administrator cannot delete their own account.");
        }

        // Self-protection: last active administrator
        if (user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.ACTIVE) {
            long activeAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.ADMIN && u.getStatus() == UserStatus.ACTIVE && !u.getId().equals(id))
                    .count();
            if (activeAdmins == 0) {
                throw new BadRequestException("Cannot delete this administrator account. At least one active administrator must remain in the system.");
            }
        }

        user.setStatus(UserStatus.DELETED);
        userRepository.delete(user); // Trigger Hibernate SQLDelete mapping
        
        // Log audit action
        auditLogService.log(
                "USER_DELETE",
                "USER",
                user.getId(),
                user.getUsername(),
                "User account soft-deleted by administrator."
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String search, String role, String status, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            var predicate = cb.conjunction();

            // Exclude DELETED users by default
            predicate = cb.and(predicate, cb.notEqual(root.get("status"), UserStatus.DELETED));

            if (search != null && !search.trim().isEmpty()) {
                String wildcard = "%" + search.toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("firstName")), wildcard),
                        cb.like(cb.lower(root.get("lastName")), wildcard),
                        cb.like(cb.lower(root.get("email")), wildcard),
                        cb.like(cb.lower(root.get("username")), wildcard),
                        cb.like(cb.lower(root.get("role").as(String.class)), wildcard)
                ));
            }

            if (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role)) {
                try {
                    UserRole roleEnum = UserRole.valueOf(role.toUpperCase());
                    predicate = cb.and(predicate, cb.equal(root.get("role"), roleEnum));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid roles
                }
            }

            if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                try {
                    UserStatus statusEnum = UserStatus.valueOf(status.toUpperCase());
                    predicate = cb.and(predicate, cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status
                }
            }

            return predicate;
        };

        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }
}
