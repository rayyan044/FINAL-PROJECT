package com.falconenergy.service.impl;

import com.falconenergy.dto.DriverAccountCreateRequest;
import com.falconenergy.dto.DriverAccountResponse;
import com.falconenergy.dto.DriverPasswordResetResponse;
import com.falconenergy.entity.Driver;
import com.falconenergy.entity.Role;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.mapper.DriverMapper;
import com.falconenergy.repository.DriverRepository;
import com.falconenergy.repository.RoleRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

    @Mock private DriverRepository driverRepository;
    @Mock private DriverMapper driverMapper;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private DriverServiceImpl driverService;

    private Driver driver;

    @BeforeEach
    void setUp() {
        driver = Driver.builder()
                .id(7L)
                .firstName("Asha")
                .lastName("Mrema")
                .phone("+255700000001")
                .licenseNumber("TZ-12345")
                .build();
    }

    @Test
    void createMobileAccount_createsActiveDriverUserWithEncodedPasswordAndAuditLog() {
        DriverAccountCreateRequest request = DriverAccountCreateRequest.builder()
                .username("asha.driver")
                .temporaryPassword("Temporary123!")
                .build();
        Role driverRole = Role.builder().roleName("DRIVER").build();

        when(driverRepository.findById(7L)).thenReturn(Optional.of(driver));
        when(userRepository.existsByUsername("asha.driver")).thenReturn(false);
        when(userRepository.findByDriverId(7L)).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("DRIVER")).thenReturn(Optional.of(driverRole));
        when(userRepository.existsByEmail("driver-asha.driver@falconenergy.local")).thenReturn(false);
        when(passwordEncoder.encode("Temporary123!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(9L);
            return user;
        });

        DriverAccountResponse response = driverService.createMobileAccount(7L, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("asha.driver", saved.getUsername());
        assertEquals("driver-asha.driver@falconenergy.local", saved.getEmail());
        assertEquals("bcrypt-hash", saved.getPassword());
        assertSame(driver, saved.getDriver());
        assertSame(driverRole, saved.getRoleEntity());
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertFalse(saved.isPasswordChanged());
        assertEquals(9L, response.getUserId());
        assertEquals(7L, response.getDriverId());
        assertEquals("Asha Mrema", response.getDriverName());
        verify(auditLogService).log(eq("DRIVER_ACCOUNT_CREATED"), eq("DRIVER"), eq(7L), eq("asha.driver"), anyString());
    }

    @Test
    void createMobileAccount_rejectsDriverThatAlreadyHasAnAccount() {
        DriverAccountCreateRequest request = DriverAccountCreateRequest.builder()
                .username("asha.driver")
                .temporaryPassword("Temporary123!")
                .build();
        when(driverRepository.findById(7L)).thenReturn(Optional.of(driver));
        when(userRepository.existsByUsername("asha.driver")).thenReturn(false);
        when(userRepository.findByDriverId(7L)).thenReturn(Optional.of(User.builder().build()));

        assertThrows(DuplicateResourceException.class, () -> driverService.createMobileAccount(7L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetMobileAccountPassword_replacesPasswordAndReturnsTemporaryPassword() {
        User account = User.builder()
                .id(9L)
                .username("asha.driver")
                .roleEntity(Role.builder().roleName(UserRole.DRIVER.name()).build())
                .driver(driver)
                .status(UserStatus.ACTIVE)
                .build();
        when(driverRepository.findById(7L)).thenReturn(Optional.of(driver));
        when(userRepository.findByDriverId(7L)).thenReturn(Optional.of(account));
        when(passwordEncoder.encode(anyString())).thenReturn("new-bcrypt-hash");

        DriverPasswordResetResponse response = driverService.resetMobileAccountPassword(7L);

        assertEquals(7L, response.getDriverId());
        assertEquals("asha.driver", response.getUsername());
        assertNotNull(response.getTemporaryPassword());
        assertEquals(16, response.getTemporaryPassword().length());
        assertEquals("new-bcrypt-hash", account.getPassword());
        assertFalse(account.isPasswordChanged());
        verify(userRepository).save(account);
        verify(auditLogService).log(eq("DRIVER_ACCOUNT_PASSWORD_RESET"), eq("DRIVER"), eq(7L), eq("asha.driver"), anyString());
    }
}
