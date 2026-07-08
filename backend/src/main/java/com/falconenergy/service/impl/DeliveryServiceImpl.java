package com.falconenergy.service.impl;

import com.falconenergy.dto.DeliveryRequest;
import com.falconenergy.dto.DeliveryResponse;
import com.falconenergy.entity.Delivery;
import com.falconenergy.entity.Driver;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.Vehicle;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.DuplicateResourceException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.mapper.DeliveryMapper;
import com.falconenergy.repository.DeliveryRepository;
import com.falconenergy.repository.DriverRepository;
import com.falconenergy.repository.FuelOrderRepository;
import com.falconenergy.repository.VehicleRepository;
import com.falconenergy.service.DeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelOrderRepository fuelOrderRepository;
    private final DeliveryMapper deliveryMapper;

    public DeliveryServiceImpl(
            DeliveryRepository deliveryRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            FuelOrderRepository fuelOrderRepository,
            DeliveryMapper deliveryMapper
    ) {
        this.deliveryRepository = deliveryRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.fuelOrderRepository = fuelOrderRepository;
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    public DeliveryResponse createDelivery(DeliveryRequest request) {
        log.info("Creating delivery: {}", request.getDeliveryNumber());
        if (deliveryRepository.existsByDeliveryNumber(request.getDeliveryNumber())) {
            throw new DuplicateResourceException("Delivery number already exists: " + request.getDeliveryNumber());
        }

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));

        if ("BUSY".equalsIgnoreCase(driver.getStatus())) {
            throw new BadRequestException("Driver is currently busy with another active delivery");
        }
        if ("INACTIVE".equalsIgnoreCase(driver.getStatus())) {
            throw new BadRequestException("Driver profile is inactive");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        if ("BUSY".equalsIgnoreCase(vehicle.getCurrentStatus())) {
            throw new BadRequestException("Vehicle is currently assigned to another active delivery");
        }
        if ("INACTIVE".equalsIgnoreCase(vehicle.getCurrentStatus())) {
            throw new BadRequestException("Vehicle profile is inactive");
        }

        FuelOrder order = fuelOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel order not found with id: " + request.getOrderId()));

        Delivery delivery = deliveryMapper.toEntity(request);
        delivery.setDriver(driver);
        delivery.setVehicle(vehicle);
        delivery.setOrder(order);
        delivery.setDeliveryStatus("PENDING");

        Delivery saved = deliveryRepository.save(delivery);

        // Mark driver as BUSY
        driver.setStatus("BUSY");
        driverRepository.save(driver);

        // Mark vehicle as BUSY
        vehicle.setCurrentStatus("BUSY");
        vehicleRepository.save(vehicle);

        // Update fuel order status to IN_TRANSIT
        order.setOrderStatus("IN_TRANSIT");
        fuelOrderRepository.save(order);

        return deliveryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryById(Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));
        return deliveryMapper.toResponse(delivery);
    }

    @Override
    public DeliveryResponse updateDelivery(Long id, DeliveryRequest request) {
        log.info("Updating delivery with id: {}", id);
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));

        if (!delivery.getDeliveryNumber().equals(request.getDeliveryNumber()) &&
                deliveryRepository.existsByDeliveryNumber(request.getDeliveryNumber())) {
            throw new DuplicateResourceException("Delivery number already exists: " + request.getDeliveryNumber());
        }

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        FuelOrder order = fuelOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel order not found with id: " + request.getOrderId()));

        // Check driver status if changed
        if (!delivery.getDriver().getId().equals(driver.getId())) {
            if ("BUSY".equalsIgnoreCase(driver.getStatus())) {
                throw new BadRequestException("New driver is currently busy with another active delivery");
            }
            Driver oldDriver = delivery.getDriver();
            oldDriver.setStatus("AVAILABLE");
            driverRepository.save(oldDriver);

            driver.setStatus("BUSY");
            driverRepository.save(driver);
        }

        // Check vehicle status if changed
        if (!delivery.getVehicle().getId().equals(vehicle.getId())) {
            if ("BUSY".equalsIgnoreCase(vehicle.getCurrentStatus())) {
                throw new BadRequestException("New vehicle is currently assigned to another active delivery");
            }
            Vehicle oldVehicle = delivery.getVehicle();
            oldVehicle.setCurrentStatus("ACTIVE");
            vehicleRepository.save(oldVehicle);

            vehicle.setCurrentStatus("BUSY");
            vehicleRepository.save(vehicle);
        }

        deliveryMapper.updateEntityFromRequest(request, delivery);
        delivery.setDriver(driver);
        delivery.setVehicle(vehicle);
        delivery.setOrder(order);

        Delivery updated = deliveryRepository.save(delivery);
        return deliveryMapper.toResponse(updated);
    }

    @Override
    public DeliveryResponse updateDeliveryStatus(Long id, String status) {
        log.info("Updating status for delivery {} to {}", id, status);
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));

        String upperStatus = status.toUpperCase();
        delivery.setDeliveryStatus(upperStatus);

        if ("EN_ROUTE".equals(upperStatus)) {
            delivery.setDepartureTime(LocalDateTime.now());
        } else if ("DELIVERED".equals(upperStatus)) {
            delivery.setArrivalTime(LocalDateTime.now());

            // Release driver
            Driver driver = delivery.getDriver();
            driver.setStatus("AVAILABLE");
            driverRepository.save(driver);

            // Release vehicle
            Vehicle vehicle = delivery.getVehicle();
            if (vehicle != null) {
                vehicle.setCurrentStatus("ACTIVE");
                vehicleRepository.save(vehicle);
            }

            // Update order status to DELIVERED
            FuelOrder order = delivery.getOrder();
            order.setOrderStatus("DELIVERED");
            order.setDeliveryDate(LocalDateTime.now());
            fuelOrderRepository.save(order);
        } else if ("CANCELLED".equals(upperStatus)) {
            // Release driver
            Driver driver = delivery.getDriver();
            driver.setStatus("AVAILABLE");
            driverRepository.save(driver);

            // Release vehicle
            Vehicle vehicle = delivery.getVehicle();
            if (vehicle != null) {
                vehicle.setCurrentStatus("ACTIVE");
                vehicleRepository.save(vehicle);
            }

            // Update order back to CANCELLED
            FuelOrder order = delivery.getOrder();
            order.setOrderStatus("CANCELLED");
            fuelOrderRepository.save(order);
        }

        Delivery updated = deliveryRepository.save(delivery);
        return deliveryMapper.toResponse(updated);
    }

    @Override
    public void deleteDelivery(Long id) {
        log.info("Deleting delivery with id: {}", id);
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with id: " + id));

        // Restore driver status
        Driver driver = delivery.getDriver();
        driver.setStatus("AVAILABLE");
        driverRepository.save(driver);

        // Restore vehicle status
        Vehicle vehicle = delivery.getVehicle();
        if (vehicle != null) {
            vehicle.setCurrentStatus("ACTIVE");
            vehicleRepository.save(vehicle);
        }

        deliveryRepository.delete(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryResponse> getAllDeliveries(
            String search,
            String status,
            Long driverId,
            Long vehicleId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Specification<Delivery> spec = Specification.where(null);

        if (search != null && !search.trim().isEmpty()) {
            String wildcard = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("deliveryNumber")), wildcard));
        }

        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("deliveryStatus")), status.toLowerCase()));
        }

        if (driverId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("driver").get("id"), driverId));
        }

        if (vehicleId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("vehicle").get("id"), vehicleId));
        }

        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("departureTime"), startDate));
        }

        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("departureTime"), endDate));
        }

        return deliveryRepository.findAll(spec, pageable).map(deliveryMapper::toResponse);
    }
}
