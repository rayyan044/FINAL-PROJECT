package com.falconenergy.service.impl;

import com.falconenergy.dto.UserRegisterRequest;
import com.falconenergy.dto.UserResponse;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.mapper.UserMapper;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
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
                .password("encodedPassword")
                .role(UserRole.ADMIN)
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
}
