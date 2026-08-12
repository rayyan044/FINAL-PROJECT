package com.falconenergy.service.impl;

import com.falconenergy.dto.VehicleRequest;
import com.falconenergy.dto.VehicleResponse;
import com.falconenergy.entity.Driver;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.entity.Vehicle;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.mapper.VehicleMapper;
import com.falconenergy.repository.DriverRepository;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.repository.VehicleRepository;
import com.falconenergy.service.VehicleService;
import com.falconenergy.service.DeliveryService;
import com.falconenergy.repository.DeliveryRepository;
import com.falconenergy.entity.Delivery;
import com.falconenergy.entity.DeliveryStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final FuelProductRepository fuelProductRepository;
    private final VehicleMapper vehicleMapper;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryService deliveryService;

    public VehicleServiceImpl(
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            FuelProductRepository fuelProductRepository,
            VehicleMapper vehicleMapper,
            DeliveryRepository deliveryRepository,
            DeliveryService deliveryService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.fuelProductRepository = fuelProductRepository;
        this.vehicleMapper = vehicleMapper;
        this.deliveryRepository = deliveryRepository;
        this.deliveryService = deliveryService;
    }

    @Override
    public VehicleResponse createVehicle(VehicleRequest request) {
        log.info("Creating vehicle with plate: {}", request.getPlateNumber());
        if (vehicleRepository.existsByPlateNumber(request.getPlateNumber())) {
            throw new DuplicateResourceException("Plate number already exists: " + request.getPlateNumber());
        }
        if (vehicleRepository.existsByTruckNumber(request.getTruckNumber())) {
            throw new DuplicateResourceException("Truck number already exists: " + request.getTruckNumber());
        }
        validateFleetFields(request);

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
        if (!vehicle.getTruckNumber().equals(request.getTruckNumber()) && vehicleRepository.existsByTruckNumber(request.getTruckNumber())) {
            throw new DuplicateResourceException("Truck number already exists: " + request.getTruckNumber());
        }
        validateFleetFields(request);

        Driver oldDriver = vehicle.getDriver();
        Driver driver = null;
        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));
        }

        vehicleMapper.updateEntityFromRequest(request, vehicle);
        vehicle.setDriver(driver);
        Vehicle updated = vehicleRepository.save(vehicle);

        Driver newDriver = updated.getDriver();
        if (newDriver != null && (oldDriver == null || !oldDriver.getId().equals(newDriver.getId()))) {
            List<Delivery> activeDeliveries = deliveryRepository.findAll().stream()
                    .filter(d -> d.getLoadingActivity() != null && 
                                 d.getLoadingActivity().getVehicle() != null &&
                                 d.getLoadingActivity().getVehicle().getId().equals(updated.getId()) &&
                                 d.getDeliveryStatus() != DeliveryStatus.COMPLETED &&
                                 d.getDeliveryStatus() != DeliveryStatus.CANCELLED)
                    .toList();
            for (Delivery delivery : activeDeliveries) {
                deliveryService.sendDeliveryAssignedNotification(delivery, newDriver);
            }
        }

        return vehicleMapper.toResponse(updated);
    }

    @Override
    public void deleteVehicle(Long id) {
        log.info("Deleting vehicle with id: {}", id);
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        vehicle.setActive(false);
        vehicle.setCurrentStatus("OUT_OF_SERVICE");
        vehicleRepository.save(vehicle);
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

    private void validateFleetFields(VehicleRequest request) {
        if (request.getAssignedFuelTypes() == null || request.getAssignedFuelTypes().isEmpty()) {
            throw new BadRequestException("At least one fuel type must be assigned to a truck.");
        }
        java.util.Set<String> configuredFuelTypes = fuelProductRepository.findAll().stream()
                .filter(product -> "ACTIVE".equalsIgnoreCase(product.getStatus()))
                .map(FuelProduct::getFuelType)
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(fuelType -> !fuelType.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> assignedFuelTypes = request.getAssignedFuelTypes().stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(fuelType -> !fuelType.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        if (assignedFuelTypes.isEmpty() || !configuredFuelTypes.containsAll(assignedFuelTypes)) {
            throw new BadRequestException("Vehicles must be assigned one or more active fuel products configured by Finance.");
        }
        request.setAssignedFuelTypes(assignedFuelTypes);
        String status = request.getCurrentStatus() == null ? "AVAILABLE" : request.getCurrentStatus().toUpperCase();
        if (!java.util.Set.of("AVAILABLE", "ASSIGNED", "DISPATCHED", "IN_TRANSIT", "DELIVERED", "MAINTENANCE", "OUT_OF_SERVICE").contains(status)) {
            throw new BadRequestException("Invalid truck status.");
        }
        request.setCurrentStatus(status);
    }
}
