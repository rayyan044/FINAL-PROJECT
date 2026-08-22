package com.falconenergy.service;

import com.falconenergy.dto.UserRegisterRequest;
import com.falconenergy.dto.UserResponse;
import com.falconenergy.entity.AuditLog;
import com.falconenergy.entity.Role;
import com.falconenergy.entity.User;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.repository.RoleRepository;
import com.falconenergy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SystemAdministrationTest {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Ensure required roles exist
        if (!roleRepository.existsByRoleName("ADMIN")) {
            roleRepository.save(Role.builder().roleName("ADMIN").description("Admin").build());
        }
        if (!roleRepository.existsByRoleName("SALES_OFFICER")) {
            roleRepository.save(Role.builder().roleName("SALES_OFFICER").description("Sales").build());
        }
        if (!roleRepository.existsByRoleName("VIEWER")) {
            roleRepository.save(Role.builder().roleName("VIEWER").description("Viewer").build());
        }
    }

    @Test
    void testUserCreationAndRoleAssignment() {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .username("testadmin")
                .firstName("Test")
                .lastName("Admin")
                .email("testadmin@falconenergy.com")
                .phone("+254711111111")
                .password("Password123")
                .confirmPassword("Password123")
                .role("ADMIN")
                .build();

        UserResponse created = userManagementService.createUser(request);
        assertNotNull(created);
        assertEquals("testadmin", created.getUsername());
        assertEquals("testadmin@falconenergy.com", created.getEmail());
        assertEquals("ADMIN", created.getRole());

        // Verify password is encrypted
        User user = userRepository.findById(created.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("Password123", user.getPassword()));

        // Assign a different role
        Role viewerRole = roleRepository.findByRoleName("VIEWER").orElseThrow();
        UserResponse updatedRole = userManagementService.assignRole(user.getId(), viewerRole.getId());
        assertEquals("VIEWER", updatedRole.getRole());
    }

    @Test
    void testDuplicateUserPrevention() {
        UserRegisterRequest request1 = UserRegisterRequest.builder()
                .username("dupuser")
                .firstName("Dup")
                .lastName("User")
                .email("dupuser@falconenergy.com")
                .password("Password123")
                .confirmPassword("Password123")
                .role("VIEWER")
                .build();

        userManagementService.createUser(request1);

        UserRegisterRequest request2 = UserRegisterRequest.builder()
                .username("dupuser")
                .firstName("Dup2")
                .lastName("User2")
                .email("otheremail@falconenergy.com")
                .password("Password123")
                .confirmPassword("Password123")
                .role("VIEWER")
                .build();

        assertThrows(DuplicateResourceException.class, () -> userManagementService.createUser(request2));
    }

    @Test
    void testUserLocking() {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .username("lockuser")
                .firstName("Lock")
                .lastName("User")
                .email("lockuser@falconenergy.com")
                .password("Password123")
                .confirmPassword("Password123")
                .role("VIEWER")
                .build();

        UserResponse created = userManagementService.createUser(request);
        assertEquals("ACTIVE", created.getStatus());

        // Lock user (set to INACTIVE)
        UserResponse locked = userManagementService.changeUserStatus(created.getId(), "INACTIVE");
        assertEquals("INACTIVE", locked.getStatus());
    }

    @Test
    void testAuditCreation() {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .username("audituser")
                .firstName("Audit")
                .lastName("User")
                .email("audituser@falconenergy.com")
                .password("Password123")
                .confirmPassword("Password123")
                .role("VIEWER")
                .build();

        UserResponse created = userManagementService.createUser(request);
        assertNotNull(created);

        // Fetch audit logs for USER
        List<AuditLog> history = auditService.getEntityHistory("USER", created.getId());
        assertFalse(history.isEmpty());
        assertEquals("USER_CREATED", history.get(0).getAction());
    }

    @Test
    void testDynamicSettingsRetrievalAndUpdating() {
        String testKey = "TEST_SETTING_KEY";
        String defaultValue = "DEFAULT_VAL";

        String value = systemSettingService.getSetting(testKey, defaultValue);
        assertEquals(defaultValue, value);

        systemSettingService.updateSetting(testKey, "NEW_DYNAMIC_VAL");
        String updatedValue = systemSettingService.getSetting(testKey);
        assertEquals("NEW_DYNAMIC_VAL", updatedValue);
    }
}
