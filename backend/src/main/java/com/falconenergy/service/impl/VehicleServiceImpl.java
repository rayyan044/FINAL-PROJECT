package com.falconenergy.service.impl;

import com.falconenergy.dto.VehicleRequest;
import com.falconenergy.dto.VehicleResponse;
import com.falconenergy.entity.Driver;
import com.falconenergy.entity.Vehicle;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.VehicleMapper;
import com.falconenergy.repository.DriverRepository;
import com.falconenergy.repository.VehicleRepository;
import com.falconenergy.service.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleServiceImpl(
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            VehicleMapper vehicleMapper
    ) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    public VehicleResponse createVehicle(VehicleRequest request) {
        log.info("Creating vehicle with plate: {}", request.getPlateNumber());
        if (vehicleRepository.existsByPlateNumber(request.getPlateNumber())) {
            throw new DuplicateResourceException("Plate number already exists: " + request.getPlateNumber());
        }

        Driver driver = null;
        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));
        }

        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setDriver(driver);
        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        return vehicleMapper.toResponse(vehicle);
    }

    @Override
    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        log.info("Updating vehicle with id: {}", id);
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (!vehicle.getPlateNumber().equals(request.getPlateNumber()) &&
                vehicleRepository.existsByPlateNumber(request.getPlateNumber())) {
            throw new DuplicateResourceException("Plate number already exists: " + request.getPlateNumber());
        }

        Driver driver = null;
        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));
        }

        vehicleMapper.updateEntityFromRequest(request, vehicle);
        vehicle.setDriver(driver);
        Vehicle updated = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(updated);
    }

    @Override
    public void deleteVehicle(Long id) {
        log.info("Deleting vehicle with id: {}", id);
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicleRepository.delete(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getAllVehicles(String search, String status, Pageable pageable) {
        Specification<Vehicle> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            String wildcard = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("plateNumber")), wildcard));
        }

        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("currentStatus")), status.toLowerCase()));
        }

        return vehicleRepository.findAll(spec, pageable).map(vehicleMapper::toResponse);
    }
}
