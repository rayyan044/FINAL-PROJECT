package com.falconenergy.service.impl;

import com.falconenergy.dto.UserRegisterRequest;
import com.falconenergy.dto.UserLoginRequest;
import com.falconenergy.dto.RefreshTokenRequest;
import com.falconenergy.dto.UserResponse;
import com.falconenergy.dto.TokenResponse;
import com.falconenergy.entity.User;
import com.falconenergy.entity.Driver;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.entity.Role;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.mapper.UserMapper;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.repository.RoleRepository;
import com.falconenergy.repository.DriverRepository;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.security.JwtTokenProvider;
import com.falconenergy.security.RefreshTokenRevocationService;
import org.junit.jupiter.api.BeforeEach;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private RefreshTokenRevocationService refreshTokenRevocationService;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegisterRequest registerRequest;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        registerRequest = UserRegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .username("johndoe")
                .password("password123")
                .confirmPassword("password123")
                .role("ADMIN")
                .phone("1234567890")
                .build();

        user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .username("johndoe")
                .password("encodedPassword")
                .roleEntity(Role.builder().roleName("ADMIN").build())
                .status(UserStatus.ACTIVE)
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .role("ADMIN")
                .status("ACTIVE")
                .build();
    }

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(Role.builder().roleName("ADMIN").build()));
        when(userMapper.toEntity(registerRequest)).thenReturn(user);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        // Act
        UserResponse response = userService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ThrowsException_WhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> userService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ActiveAccount_ReturnsCompleteTokenResponse() {
        UserLoginRequest request = UserLoginRequest.builder()
                .email("john.doe@example.com")
                .password("password123")
                .build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        TokenResponse response = userService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals("johndoe", response.getUsername());
        assertEquals("ADMIN", response.getRole());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_DriverAccount_ReturnsLinkedDriverDetails() {
        UserLoginRequest request = UserLoginRequest.builder()
                .email("john.doe@example.com")
                .password("password123")
                .build();
        user.setRoleEntity(Role.builder().roleName(UserRole.DRIVER.name()).build());
        user.setDriver(Driver.builder().id(42L).firstName("Asha").lastName("Mrema").build());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        TokenResponse response = userService.login(request);

        assertEquals("DRIVER", response.getRole());
        assertEquals(42L, response.getDriverId());
        assertEquals("Asha Mrema", response.getDriverName());
    }

    @Test
    void login_UnknownEmail_ReturnsBadCredentials() {
        UserLoginRequest request = UserLoginRequest.builder()
                .email("unknown@example.com")
                .password("password123")
                .build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> userService.login(request));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_InactiveAccount_ReturnsDisabledExceptionBeforeAuthentication() {
        UserLoginRequest request = UserLoginRequest.builder()
                .email("john.doe@example.com")
                .password("password123")
                .build();
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> userService.login(request));
        verify(authenticationManager, never()).authenticate(any());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void refreshToken_InactiveAccount_ReturnsDisabledException() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refresh-token")
                .build();
        user.setStatus(UserStatus.INACTIVE);
        when(jwtTokenProvider.extractUsername("refresh-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> userService.refreshToken(request));
        verify(jwtTokenProvider, never()).isTokenValid(anyString(), any());
        verify(jwtTokenProvider, never()).generateToken(any());
    }
}
