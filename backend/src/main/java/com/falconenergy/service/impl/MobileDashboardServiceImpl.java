package com.falconenergy.service.impl;

import com.falconenergy.dto.MobileDashboardResponse;
import com.falconenergy.entity.Delivery;
import com.falconenergy.entity.DeliveryNote;
import com.falconenergy.entity.DeliveryStatus;
import com.falconenergy.entity.DispatchStatus;
import com.falconenergy.entity.Driver;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.Vehicle;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.DeliveryRepository;
import com.falconenergy.repository.DispatchRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.repository.VehicleRepository;
import com.falconenergy.repository.projection.MobileDeliveryCounts;
import com.falconenergy.service.MobileDashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MobileDashboardServiceImpl implements MobileDashboardService {

    private static final int RECENT_DELIVERIES_LIMIT = 10;

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryRepository deliveryRepository;
    private final DispatchRepository dispatchRepository;

    public MobileDashboardServiceImpl(UserRepository userRepository, VehicleRepository vehicleRepository,
                                      DeliveryRepository deliveryRepository, DispatchRepository dispatchRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.deliveryRepository = deliveryRepository;
        this.dispatchRepository = dispatchRepository;
    }

    @Override
    public MobileDashboardResponse getDashboard() {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.DRIVER) {
            throw new AccessDeniedException("Only driver accounts can access the mobile dashboard");
        }
        Driver driver = user.getDriver();
        if (driver == null) {
            throw new BadRequestException("Driver account is not linked to a driver profile");
        }
        if ("INACTIVE".equalsIgnoreCase(driver.getStatus())) {
            throw new DisabledException("Driver profile is inactive");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        MobileDeliveryCounts counts = deliveryRepository.getMobileDashboardCounts(
                driver.getId(), List.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.ARRIVED_AT_DESTINATION),
                DeliveryStatus.DELIVERED, todayStart, tomorrowStart);
        long pendingDeliveries = dispatchRepository.countPendingMobileDeliveries(
                driver.getId(), List.of(DispatchStatus.READY, DispatchStatus.DISPATCHED));
        List<Delivery> recentDeliveries = deliveryRepository.findRecentForMobileDriver(
                driver.getId(), PageRequest.of(0, RECENT_DELIVERIES_LIMIT));

        return MobileDashboardResponse.builder()
                .driver(toDriverInfo(driver, vehicleRepository.findByDriverId(driver.getId()).orElse(null)))
                .summary(MobileDashboardResponse.Summary.builder()
                        .assignedDeliveries(pendingDeliveries + counts.deliveriesInProgress())
                        .pendingDeliveries(pendingDeliveries)
                        .deliveriesInProgress(counts.deliveriesInProgress())
                        .completedToday(counts.completedToday())
                        .build())
                .recentDeliveries(recentDeliveries.stream().map(this::toRecentDelivery).toList())
                // Notifications are not yet persisted in this backend; expose a stable zero value for mobile clients.
                .notifications(MobileDashboardResponse.NotificationSummary.builder().unreadCount(0).build())
                .build();
    }

    @Override
    public List<MobileDashboardResponse.RecentDelivery> getDeliveries() {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.DRIVER) {
            throw new AccessDeniedException("Only driver accounts can access the mobile deliveries list");
        }
        Driver driver = user.getDriver();
        if (driver == null) {
            throw new BadRequestException("Driver account is not linked to a driver profile");
        }
        if ("INACTIVE".equalsIgnoreCase(driver.getStatus())) {
            throw new DisabledException("Driver profile is inactive");
        }

        List<Delivery> deliveries = deliveryRepository.findRecentForMobileDriver(
                driver.getId(), PageRequest.of(0, 100));

        return deliveries.stream().map(this::toRecentDelivery).toList();
    }

    @Override
    public MobileDashboardResponse.RecentDelivery getDelivery(Long deliveryId) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.DRIVER || user.getDriver() == null) {
            throw new AccessDeniedException("Only linked driver accounts can access mobile deliveries");
        }
        Delivery delivery = deliveryRepository.findForMobileDriver(deliveryId, user.getDriver().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for this driver"));
        return toRecentDelivery(delivery);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Authentication is required");
        }
        return userRepository.findByEmail(authentication.getName())
                .or(() -> userRepository.findByUsername(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }

    private MobileDashboardResponse.DriverInfo toDriverInfo(Driver driver, Vehicle vehicle) {
        return MobileDashboardResponse.DriverInfo.builder()
                .driverId(driver.getId())
                .driverName(driver.getFirstName() + " " + driver.getLastName())
                .assignedVehicle(vehicle == null ? null : MobileDashboardResponse.VehicleInfo.builder()
                        .vehicleId(vehicle.getId())
                        .truckNumber(vehicle.getTruckNumber())
                        .plateNumber(vehicle.getPlateNumber())
                        .status(vehicle.getCurrentStatus())
                        .build())
                .build();
    }

    private MobileDashboardResponse.RecentDelivery toRecentDelivery(Delivery delivery) {
        DeliveryNote note = delivery.getDeliveryNote();
        FuelOrder order = delivery.getLoadingOrder() == null ? null : delivery.getLoadingOrder().getOrder();
        BigDecimal quantity = note != null && note.getStandardVolume() != null
                ? note.getStandardVolume()
                : delivery.getLoadingActivity() == null ? null : delivery.getLoadingActivity().getAllocatedQuantity();
        return MobileDashboardResponse.RecentDelivery.builder()
                .deliveryId(delivery.getId())
                .deliveryNoteNumber(note == null ? null : note.getDeliveryNoteNumber())
                .customerName(note != null && note.getCustomer() != null ? note.getCustomer().getCompanyName()
                        : order != null && order.getCustomer() != null ? order.getCustomer().getCompanyName() : null)
                .fuelProduct(note != null && note.getProduct() != null ? note.getProduct().getProductName()
                        : order != null && order.getProduct() != null ? order.getProduct().getProductName() : null)
                .quantity(quantity)
                .destination(delivery.getDestination())
                .currentStatus(delivery.getDeliveryStatus().name())
                .scheduledDeliveryDate(delivery.getLoadingOrder() == null ? null : delivery.getLoadingOrder().getLoadingDate())
                .build();
    }
}
