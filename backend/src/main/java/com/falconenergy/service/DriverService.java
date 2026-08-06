package com.falconenergy.service;

import com.falconenergy.dto.DriverRequest;
import com.falconenergy.dto.DriverResponse;
import com.falconenergy.dto.DriverAccountCreateRequest;
import com.falconenergy.dto.DriverAccountResponse;
import com.falconenergy.dto.DriverPasswordResetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DriverService {
    DriverResponse createDriver(DriverRequest request);
    DriverResponse getDriverById(Long id);
    DriverResponse updateDriver(Long id, DriverRequest request);
    void deleteDriver(Long id);
    Page<DriverResponse> getAllDrivers(String search, String status, Pageable pageable);
    DriverAccountResponse createMobileAccount(Long driverId, DriverAccountCreateRequest request);
    DriverPasswordResetResponse resetMobileAccountPassword(Long driverId);
    DriverAccountResponse setMobileAccountEnabled(Long driverId, boolean enabled);
    DriverAccountResponse getMobileAccountStatus(Long driverId);
}
