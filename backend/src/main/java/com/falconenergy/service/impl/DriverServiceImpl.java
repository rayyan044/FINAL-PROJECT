package com.falconenergy.service.impl;

import com.falconenergy.dto.DriverRequest;
import com.falconenergy.dto.DriverResponse;
import com.falconenergy.entity.Driver;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.DriverMapper;
import com.falconenergy.repository.DriverRepository;
import com.falconenergy.service.DriverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    public DriverServiceImpl(DriverRepository driverRepository, DriverMapper driverMapper) {
        this.driverRepository = driverRepository;
        this.driverMapper = driverMapper;
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
}
