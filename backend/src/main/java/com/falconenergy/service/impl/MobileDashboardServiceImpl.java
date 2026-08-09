package com.falconenergy.service.impl;

import com.falconenergy.dto.DriverProfileResponse;
import com.falconenergy.dto.MobileDashboardResponse;
import com.falconenergy.dto.NotificationResponse;
import com.falconenergy.entity.*;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.exception.ResourceNotFoundException;
import com.falconenergy.repository.*;
import com.falconenergy.repository.projection.MobileDeliveryCounts;
import com.falconenergy.service.DeliveryService;
import com.falconenergy.service.DispatchService;
import com.falconenergy.service.MobileDashboardService;
import com.falconenergy.service.ProofOfDeliveryStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final NotificationRepository notificationRepository;
    private final ProofOfDeliveryStorageService proofOfDeliveryStorageService;

    private final DeliveryService deliveryService;
    private final DispatchService dispatchService;

    public MobileDashboardServiceImpl(UserRepository userRepository, VehicleRepository vehicleRepository,
                                      DeliveryRepository deliveryRepository, DispatchRepository dispatchRepository,
                                      NotificationRepository notificationRepository,
                                      ProofOfDeliveryStorageService proofOfDeliveryStorageService,
                                      @Lazy DeliveryService deliveryService,
                                      @Lazy DispatchService dispatchService) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.deliveryRepository = deliveryRepository;
        this.dispatchRepository = dispatchRepository;
        this.notificationRepository = notificationRepository;
        this.proofOfDeliveryStorageService = proofOfDeliveryStorageService;
        this.deliveryService = deliveryService;
        this.dispatchService = dispatchService;
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
        
        // Active/In-progress statuses in our lifecycle are: IN_TRANSIT, ARRIVED_AT_DESTINATION, and DELIVERED
        MobileDeliveryCounts counts = deliveryRepository.getMobileDashboardCounts(
                driver.getId(), 
                List.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.ARRIVED_AT_DESTINATION, DeliveryStatus.DELIVERED),
                DeliveryStatus.COMPLETED, 
                todayStart, 
                tomorrowStart);
                
        long pendingDeliveries = dispatchRepository.countPendingMobileDeliveries(
                driver.getId(), List.of(DispatchStatus.READY, DispatchStatus.DISPATCHED));
        List<Delivery> recentDeliveries = deliveryRepository.findRecentForMobileDriver(
                driver.getId(), PageRequest.of(0, RECENT_DELIVERIES_LIMIT));

        long unreadNotifications = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

        return MobileDashboardResponse.builder()
                .driver(toDriverInfo(driver, vehicleRepository.findByDriverId(driver.getId()).orElse(null)))
                .summary(MobileDashboardResponse.Summary.builder()
                        .assignedDeliveries(pendingDeliveries + counts.deliveriesInProgress())
                        .pendingDeliveries(pendingDeliveries)
                        .deliveriesInProgress(counts.deliveriesInProgress())
                        .completedToday(counts.completedToday())
                        .build())
                .recentDeliveries(recentDeliveries.stream().map(this::toRecentDelivery).toList())
                .notifications(MobileDashboardResponse.NotificationSummary.builder()
                        .unreadCount(unreadNotifications)
                        .build())
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

    @Override
    @Transactional
    public void acceptDelivery(Long deliveryId) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.DRIVER || user.getDriver() == null) {
            throw new AccessDeniedException("Only linked driver accounts can accept deliveries");
        }
        Delivery delivery = deliveryRepository.findForMobileDriver(deliveryId, user.getDriver().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for this driver"));

        if (delivery.getDeliveryStatus() != DeliveryStatus.ASSIGNED) {
            throw new BadRequestException("Delivery can only be accepted if the status is ASSIGNED. Current status: " + delivery.getDeliveryStatus());
        }

        delivery.setDeliveryStatus(DeliveryStatus.ACCEPTED);
        delivery.setUpdatedBy(user.getUsername());
        deliveryRepository.save(delivery);
    }

    @Override
    @Transactional
    public void startTrip(Long deliveryId, Double latitude, Double longitude) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.DRIVER || user.getDriver() == null) {
            throw new AccessDeniedException("Only linked driver accounts can start trips");
        }
        Delivery delivery = deliveryRepository.findForMobileDriver(deliveryId, user.getDriver().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for this driver"));

        if (delivery.getDeliveryStatus() != DeliveryStatus.ACCEPTED) {
            throw new BadRequestException("Trip can only be started if the status is ACCEPTED. Current status: " + delivery.getDeliveryStatus());
        }

        // Start transit on the dispatch (which updates Dispatch, LoadingActivity, Vehicle, and LoadingOrder statuses to IN_TRANSIT, and sets delivery status to IN_TRANSIT & dispatchedAt)
        dispatchService.startTransit(delivery.getDispatch().getId());

        // Refresh delivery reference and update start coordinates
        Delivery activeDelivery = deliveryRepository.findById(deliveryId).orElseThrow();
        activeDelivery.setStartLatitude(latitude);
        activeDelivery.setStartLongitude(longitude);
        activeDelivery.setUpdatedBy(user.getUsername());
        deliveryRepository.save(activeDelivery);
    }

    @Override
    @Transactional
    public void arriveAtDestination(Long deliveryId, String receivedBy, String remarks) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.DRIVER || user.getDriver() == null) {
            throw new AccessDeniedException("Only linked driver accounts can mark arrival");
        }
        Delivery delivery = deliveryRepository.findForMobileDriver(deliveryId, user.getDriver().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for this driver"));

        if (delivery.getDeliveryStatus() != DeliveryStatus.IN_TRANSIT) {
            throw new BadRequestException("Arrival can only be recorded if the status is IN_TRANSIT. Current status: " + delivery.getDeliveryStatus());
        }

        delivery.setDeliveryStatus(DeliveryStatus.ARRIVED_AT_DESTINATION);
        delivery.setArrivalTime(LocalDateTime.now());
        delivery.setReceivedBy(receivedBy != null && !receivedBy.trim().isEmpty() ? receivedBy : user.getUsername());
        if (remarks != null) {
            delivery.setRemarks(remarks);
        }
        delivery.setUpdatedBy(user.getUsername());
        deliveryRepository.save(delivery);
    }

    @Override
    @Transactional
    public void uploadProof(Long deliveryId, MultipartFile file, Double latitude, Double longitude, String notes) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.DRIVER || user.getDriver() == null) {
            throw new AccessDeniedException("Only linked driver accounts can upload proof of delivery");
        }
        Delivery delivery = deliveryRepository.findForMobileDriver(deliveryId, user.getDriver().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for this driver"));

        if (delivery.getDeliveryStatus() != DeliveryStatus.ARRIVED_AT_DESTINATION) {
            throw new BadRequestException("Proof of delivery can only be uploaded if the status is ARRIVED_AT_DESTINATION. Current status: " + delivery.getDeliveryStatus());
        }

        // Store file securely (validates file size, content-type, prevents traversal)
        String filename = proofOfDeliveryStorageService.storeFile(file, deliveryId);

        delivery.setPodPhotoPath(filename);
        delivery.setPodLatitude(latitude);
        delivery.setPodLongitude(longitude);
        delivery.setPodNotes(notes);
        delivery.setPodUploadedAt(LocalDateTime.now());
        delivery.setDeliveryStatus(DeliveryStatus.DELIVERED); // Uploading POD moves status to DELIVERED
        delivery.setUpdatedBy(user.getUsername());
        deliveryRepository.save(delivery);
    }

    @Override
    @Transactional
    public void completeDelivery(Long deliveryId) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.DRIVER || user.getDriver() == null) {
            throw new AccessDeniedException("Only linked driver accounts can complete deliveries");
        }
        Delivery delivery = deliveryRepository.findForMobileDriver(deliveryId, user.getDriver().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for this driver"));

        if (delivery.getDeliveryStatus() != DeliveryStatus.DELIVERED) {
            throw new BadRequestException("Delivery can only be completed if the status is DELIVERED. Current status: " + delivery.getDeliveryStatus());
        }

        if (delivery.getPodPhotoPath() == null || delivery.getPodPhotoPath().isEmpty()) {
            throw new BadRequestException("Cannot complete delivery: Proof of Delivery has not been uploaded.");
        }

        // Invoke existing completeDelivery implementation (frees vehicle, updates LoadingActivity status)
        deliveryService.completeDelivery(deliveryId, null);
    }

    @Override
    public DriverProfileResponse getProfile() {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.DRIVER || user.getDriver() == null) {
            throw new AccessDeniedException("Only linked driver accounts can access driver profiles");
        }
        Driver driver = user.getDriver();
        Vehicle vehicle = vehicleRepository.findByDriverId(driver.getId()).orElse(null);

        return DriverProfileResponse.builder()
                .driverId(driver.getId())
                .username(user.getUsername())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(user.getEmail())
                .phone(driver.getPhone())
                .licenseNumber(driver.getLicenseNumber())
                .driverStatus(driver.getStatus())
                .assignedVehicle(vehicle == null ? null : MobileDashboardResponse.VehicleInfo.builder()
                        .vehicleId(vehicle.getId())
                        .truckNumber(vehicle.getTruckNumber())
                        .plateNumber(vehicle.getPlateNumber())
                        .status(vehicle.getCurrentStatus())
                        .build())
                .build();
    }

    @Override
    public List<NotificationResponse> getNotifications() {
        User user = getAuthenticatedUser();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return notifications.stream().map(n -> NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build()).toList();
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Long notificationId) {
        User user = getAuthenticatedUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have access to this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
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
                .startLatitude(delivery.getStartLatitude())
                .startLongitude(delivery.getStartLongitude())
                .podLatitude(delivery.getPodLatitude())
                .podLongitude(delivery.getPodLongitude())
                .podPhotoPath(delivery.getPodPhotoPath())
                .podNotes(delivery.getPodNotes())
                .podUploadedAt(delivery.getPodUploadedAt())
                .build();
    }
}
