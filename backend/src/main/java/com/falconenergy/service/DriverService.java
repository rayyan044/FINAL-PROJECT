package com.falconenergy.service;

import com.falconenergy.dto.DriverRequest;
import com.falconenergy.dto.DriverResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DriverService {
    DriverResponse createDriver(DriverRequest request);
    DriverResponse getDriverById(Long id);
    DriverResponse updateDriver(Long id, DriverRequest request);
    void deleteDriver(Long id);
    Page<DriverResponse> getAllDrivers(String search, String status, Pageable pageable);
}
