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
import com.falconenergy.service.ProofOfDeliveryStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileDashboardServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DispatchRepository dispatchRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ProofOfDeliveryStorageService proofOfDeliveryStorageService;
    @Mock private DeliveryService deliveryService;
    @Mock private DispatchService dispatchService;
    @Mock private MultipartFile multipartFile;

    @InjectMocks private MobileDashboardServiceImpl mobileDashboardService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsDriver(Long driverId, Long userId) {
        Driver driver = Driver.builder().id(driverId).firstName("Asha").lastName("Mrema").status("AVAILABLE").build();
        User user = User.builder().id(userId).username("asha").email("asha@falconenergy.local").status(UserStatus.ACTIVE)
                .roleEntity(Role.builder().roleName("DRIVER").build()).driver(driver).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(), "token", List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))));
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        lenient().when(userRepository.findByUsername(user.getEmail())).thenReturn(Optional.of(user));
    }

    private Delivery createMockDelivery(Long id, DeliveryStatus status, Long driverId) {
        Driver driver = Driver.builder().id(driverId).build();
        Vehicle vehicle = Vehicle.builder().driver(driver).build();
        LoadingActivity activity = LoadingActivity.builder().vehicle(vehicle).build();
        return Delivery.builder()
                .id(id)
                .deliveryStatus(status)
                .loadingActivity(activity)
                .build();
    }

    @Test
    void dashboardUsesOnlyTheDriverLinkedToTheAuthenticatedJwtUser() {
        authenticateAsDriver(17L, 101L);
        when(vehicleRepository.findByDriverId(17L)).thenReturn(Optional.of(Vehicle.builder()
                .id(23L).truckNumber("FAL-023").plateNumber("T 123 ABC").currentStatus("ASSIGNED").build()));
        when(deliveryRepository.getMobileDashboardCounts(eq(17L), anyList(), any(), any(), any()))
                .thenReturn(new MobileDeliveryCounts(2L, 1L));
        when(dispatchRepository.countPendingMobileDeliveries(eq(17L), anyList())).thenReturn(3L);
        when(deliveryRepository.findRecentForMobileDriver(eq(17L), any())).thenReturn(List.of());
        when(notificationRepository.countByUserIdAndIsReadFalse(101L)).thenReturn(4L);

        MobileDashboardResponse response = mobileDashboardService.getDashboard();

        assertEquals(17L, response.getDriver().getDriverId());
        assertEquals("Asha Mrema", response.getDriver().getDriverName());
        assertEquals("FAL-023", response.getDriver().getAssignedVehicle().getTruckNumber());
        assertEquals(5L, response.getSummary().getAssignedDeliveries());
        assertEquals(3L, response.getSummary().getPendingDeliveries());
        assertEquals(2L, response.getSummary().getDeliveriesInProgress());
        assertEquals(1L, response.getSummary().getCompletedToday());
        assertEquals(4L, response.getNotifications().getUnreadCount());
    }

    @Test
    void getDeliveriesReturnsDriverDeliveries() {
        authenticateAsDriver(17L, 101L);
        when(deliveryRepository.findRecentForMobileDriver(eq(17L), any())).thenReturn(List.of());

        List<MobileDashboardResponse.RecentDelivery> result = mobileDashboardService.getDeliveries();

        assertEquals(0, result.size());
    }

    @Test
    void deliveryNotFoundThrowsResourceNotFound() {
        authenticateAsDriver(17L, 101L);
        when(deliveryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> mobileDashboardService.getDelivery(99L));
    }

    @Test
    void driverCannotAccessAnotherDriversDelivery() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = createMockDelivery(99L, DeliveryStatus.ASSIGNED, 18L);
        when(deliveryRepository.findById(99L)).thenReturn(Optional.of(delivery));

        assertThrows(AccessDeniedException.class, () -> mobileDashboardService.getDelivery(99L));
    }

    @Test
    void acceptDeliveryTransitionsFromAssignedToAccepted() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = createMockDelivery(99L, DeliveryStatus.ASSIGNED, 17L);
        when(deliveryRepository.findById(99L)).thenReturn(Optional.of(delivery));

        mobileDashboardService.acceptDelivery(99L);

        assertEquals(DeliveryStatus.ACCEPTED, delivery.getDeliveryStatus());
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void acceptDeliveryRejectedIfStatusNotAssigned() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = createMockDelivery(99L, DeliveryStatus.IN_TRANSIT, 17L);
        when(deliveryRepository.findById(99L)).thenReturn(Optional.of(delivery));

        assertThrows(BadRequestException.class, () -> mobileDashboardService.acceptDelivery(99L));
    }

    @Test
    void startTripTransitionsFromAcceptedToInTransit() {
        authenticateAsDriver(17L, 101L);
        Dispatch dispatch = Dispatch.builder().id(200L).build();
        Delivery delivery = createMockDelivery(99L, DeliveryStatus.ACCEPTED, 17L);
        delivery.setDispatch(dispatch);
        when(deliveryRepository.findById(99L)).thenReturn(Optional.of(delivery));

        mobileDashboardService.startTrip(99L, -6.123, 39.456);

        verify(dispatchService).startTransit(200L);
        assertEquals(-6.123, delivery.getStartLatitude());
        assertEquals(39.456, delivery.getStartLongitude());
    }

    @Test
    void startTripRejectedIfStatusNotAccepted() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = createMockDelivery(99L, DeliveryStatus.ASSIGNED, 17L);
        when(deliveryRepository.findById(99L)).thenReturn(Optional.of(delivery));

        assertThrows(BadRequestException.class, () -> mobileDashboardService.startTrip(99L, 0.0, 0.0));
    }

    @Test
    void arriveAtDestinationTransitionsFromInTransitToArrived() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = Delivery.builder().id(99L).deliveryStatus(DeliveryStatus.IN_TRANSIT).build();
        when(deliveryRepository.findForMobileDriver(99L, 17L)).thenReturn(Optional.of(delivery));

        mobileDashboardService.arriveAtDestination(99L, "Kassim", "On-site");

        assertEquals(DeliveryStatus.ARRIVED_AT_DESTINATION, delivery.getDeliveryStatus());
        assertEquals("Kassim", delivery.getReceivedBy());
        assertEquals("On-site", delivery.getRemarks());
        assertNotNull(delivery.getArrivalTime());
    }

    @Test
    void arriveAtDestinationRejectedIfStatusNotInTransit() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = Delivery.builder().id(99L).deliveryStatus(DeliveryStatus.ACCEPTED).build();
        when(deliveryRepository.findForMobileDriver(99L, 17L)).thenReturn(Optional.of(delivery));

        assertThrows(BadRequestException.class, () -> mobileDashboardService.arriveAtDestination(99L, "Kassim", "On-site"));
    }

    @Test
    void uploadProofTransitionsToDelivered() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = Delivery.builder().id(99L).deliveryStatus(DeliveryStatus.ARRIVED_AT_DESTINATION).build();
        when(deliveryRepository.findForMobileDriver(99L, 17L)).thenReturn(Optional.of(delivery));
        when(proofOfDeliveryStorageService.storeFile(any(), eq(99L))).thenReturn("pod-99-uuid.jpg");

        mobileDashboardService.uploadProof(99L, multipartFile, -6.123, 39.456, "Seal intact");

        assertEquals(DeliveryStatus.DELIVERED, delivery.getDeliveryStatus());
        assertEquals("pod-99-uuid.jpg", delivery.getPodPhotoPath());
        assertEquals("Seal intact", delivery.getPodNotes());
        assertEquals(-6.123, delivery.getPodLatitude());
        assertEquals(39.456, delivery.getPodLongitude());
        assertNotNull(delivery.getPodUploadedAt());
    }

    @Test
    void uploadProofRejectedIfStatusNotArrived() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = Delivery.builder().id(99L).deliveryStatus(DeliveryStatus.IN_TRANSIT).build();
        when(deliveryRepository.findForMobileDriver(99L, 17L)).thenReturn(Optional.of(delivery));

        assertThrows(BadRequestException.class, () -> mobileDashboardService.uploadProof(99L, multipartFile, 0.0, 0.0, "Notes"));
    }

    @Test
    void completeDeliveryInvokesDeliveryServiceComplete() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = Delivery.builder().id(99L).deliveryStatus(DeliveryStatus.DELIVERED).podPhotoPath("pod-photo.jpg").build();
        when(deliveryRepository.findForMobileDriver(99L, 17L)).thenReturn(Optional.of(delivery));

        mobileDashboardService.completeDelivery(99L);

        verify(deliveryService).completeDelivery(eq(99L), any());
    }

    @Test
    void completeDeliveryRejectedIfNoPodUploaded() {
        authenticateAsDriver(17L, 101L);
        Delivery delivery = Delivery.builder().id(99L).deliveryStatus(DeliveryStatus.DELIVERED).podPhotoPath(null).build();
        when(deliveryRepository.findForMobileDriver(99L, 17L)).thenReturn(Optional.of(delivery));

        assertThrows(BadRequestException.class, () -> mobileDashboardService.completeDelivery(99L));
    }

    @Test
    void getProfileReturnsDetailedDriverProfile() {
        authenticateAsDriver(17L, 101L);
        when(vehicleRepository.findByDriverId(17L)).thenReturn(Optional.of(Vehicle.builder()
                .id(23L).truckNumber("FAL-023").plateNumber("T 123 ABC").currentStatus("ASSIGNED").build()));

        DriverProfileResponse profile = mobileDashboardService.getProfile();

        assertEquals(17L, profile.getDriverId());
        assertEquals("asha", profile.getUsername());
        assertEquals("asha@falconenergy.local", profile.getEmail());
        assertEquals("FAL-023", profile.getAssignedVehicle().getTruckNumber());
    }

    @Test
    void getNotificationsListsNotificationsForDriver() {
        authenticateAsDriver(17L, 101L);
        Notification notification = Notification.builder()
                .id(50L).title("New Assignment").message("Delivery ready").isRead(false).createdAt(LocalDateTime.now()).build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(101L)).thenReturn(List.of(notification));

        List<NotificationResponse> list = mobileDashboardService.getNotifications();

        assertEquals(1, list.size());
        assertEquals("New Assignment", list.getFirst().getTitle());
        assertFalse(list.getFirst().isRead());
    }

    @Test
    void markNotificationAsReadUpdatesReadFlag() {
        authenticateAsDriver(17L, 101L);
        User user = User.builder().id(101L).build();
        Notification notification = Notification.builder().id(50L).user(user).isRead(false).build();
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(notification));

        mobileDashboardService.markNotificationAsRead(50L);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }
}
