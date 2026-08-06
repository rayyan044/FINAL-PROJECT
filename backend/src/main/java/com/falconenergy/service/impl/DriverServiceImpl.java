package com.falconenergy.service.impl;

import com.falconenergy.dto.DriverAccountCreateRequest;
import com.falconenergy.dto.DriverAccountResponse;
import com.falconenergy.dto.DriverPasswordResetResponse;
import com.falconenergy.dto.DriverRequest;
import com.falconenergy.dto.DriverResponse;
import com.falconenergy.entity.Driver;
import com.falconenergy.entity.Role;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.DriverMapper;
import com.falconenergy.repository.DriverRepository;
import com.falconenergy.repository.RoleRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.service.AuditLogService;
import com.falconenergy.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@Transactional
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private static final char[] TEMPORARY_PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public DriverServiceImpl(
            DriverRepository driverRepository,
            DriverMapper driverMapper,
            UserRepository userRepository,
            RoleRepository roleRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.driverRepository = driverRepository;
        this.driverMapper = driverMapper;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    public DriverResponse createDriver(DriverRequest request) {
        log.info("Creating driver: {} {}", request.getFirstName(), request.getLastName());
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("License number already exists: " + request.getLicenseNumber());
        }
        Driver driver = driverMapper.toEntity(request);
        Driver saved = driverRepository.save(driver);
        return driverMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        return driverMapper.toResponse(driver);
    }

    @Override
    public DriverResponse updateDriver(Long id, DriverRequest request) {
        log.info("Updating driver with id: {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));

        if (!driver.getLicenseNumber().equals(request.getLicenseNumber()) &&
                driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("License number already exists: " + request.getLicenseNumber());
        }

        driverMapper.updateEntityFromRequest(request, driver);
        Driver updated = driverRepository.save(driver);
        return driverMapper.toResponse(updated);
    }

    @Override
    public void deleteDriver(Long id) {
        log.info("Deleting driver with id: {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        driverRepository.delete(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DriverResponse> getAllDrivers(String search, String status, Pageable pageable) {
        Specification<Driver> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            String wildcard = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), wildcard),
                    cb.like(cb.lower(root.get("lastName")), wildcard),
                    cb.like(cb.lower(root.get("licenseNumber")), wildcard)
            ));
        }

        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }

        return driverRepository.findAll(spec, pageable).map(driverMapper::toResponse);
    }

    @Override
    public DriverAccountResponse createMobileAccount(Long driverId, DriverAccountCreateRequest request) {
        Driver driver = getDriver(driverId);
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists: " + username);
        }
        if (userRepository.findByDriverId(driverId).isPresent()) {
            throw new DuplicateResourceException("Driver already has a mobile account");
        }

        Role driverRole = roleRepository.findByRoleName("DRIVER")
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: DRIVER"));
        String generatedEmail = driverAccountEmail(username);
        if (userRepository.existsByEmail(generatedEmail)) {
            throw new DuplicateResourceException("Unable to create a unique account email for this username");
        }

        User user = User.builder()
                .username(username)
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .fullName(driver.getFirstName() + " " + driver.getLastName())
                .email(generatedEmail)
                .phone(driver.getPhone())
                .password(passwordEncoder.encode(request.getTemporaryPassword()))
                .roleEntity(driverRole)
                .driver(driver)
                .status(UserStatus.ACTIVE)
                .passwordChanged(false)
                .build();

        User saved = userRepository.save(user);
        auditLogService.log("DRIVER_ACCOUNT_CREATED", "DRIVER", driverId, saved.getUsername(),
                "Mobile account created for driver " + driverId + ".");
        return toAccountResponse(saved);
    }

    @Override
    public DriverPasswordResetResponse resetMobileAccountPassword(Long driverId) {
        User user = getDriverAccount(driverId);
        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setPasswordChanged(false);
        userRepository.save(user);

        auditLogService.log("DRIVER_ACCOUNT_PASSWORD_RESET", "DRIVER", driverId, user.getUsername(),
                "Mobile account password reset for driver " + driverId + ".");
        return DriverPasswordResetResponse.builder()
                .driverId(driverId)
                .username(user.getUsername())
                .temporaryPassword(temporaryPassword)
                .build();
    }

    @Override
    public DriverAccountResponse setMobileAccountEnabled(Long driverId, boolean enabled) {
        User user = getDriverAccount(driverId);
        user.setStatus(enabled ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        User saved = userRepository.save(user);
        auditLogService.log(enabled ? "DRIVER_ACCOUNT_ENABLED" : "DRIVER_ACCOUNT_DISABLED", "DRIVER", driverId,
                saved.getUsername(), "Mobile account " + (enabled ? "enabled" : "disabled") + " for driver " + driverId + ".");
        return toAccountResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverAccountResponse getMobileAccountStatus(Long driverId) {
        return toAccountResponse(getDriverAccount(driverId));
    }

    private Driver getDriver(Long driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + driverId));
    }

    private User getDriverAccount(Long driverId) {
        getDriver(driverId);
        User user = userRepository.findByDriverId(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver mobile account not found for driver id: " + driverId));
        if (user.getRole() != UserRole.DRIVER) {
            throw new BadRequestException("The linked user account is not a DRIVER account");
        }
        return user;
    }

    private DriverAccountResponse toAccountResponse(User user) {
        Driver driver = user.getDriver();
        boolean enabled = user.getStatus() == UserStatus.ACTIVE;
        return DriverAccountResponse.builder()
                .userId(user.getId())
                .driverId(driver.getId())
                .driverName(driver.getFirstName() + " " + driver.getLastName())
                .username(user.getUsername())
                .accountStatus(enabled ? "ACTIVE" : "DISABLED")
                .enabled(enabled)
                .build();
    }

    private String driverAccountEmail(String username) {
        return "driver-" + username.trim().toLowerCase().replaceAll("[^a-z0-9._-]", "-") + "@falconenergy.local";
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            password.append(TEMPORARY_PASSWORD_CHARACTERS[SECURE_RANDOM.nextInt(TEMPORARY_PASSWORD_CHARACTERS.length)]);
        }
        return password.toString();
    }
}
