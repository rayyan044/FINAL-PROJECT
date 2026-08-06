package com.falconenergy.service.impl;

import com.falconenergy.dto.MobileDashboardResponse;
import com.falconenergy.entity.Driver;
import com.falconenergy.entity.Role;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.entity.Vehicle;
import com.falconenergy.repository.DeliveryRepository;
import com.falconenergy.repository.DispatchRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.repository.VehicleRepository;
import com.falconenergy.repository.projection.MobileDeliveryCounts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileDashboardServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DispatchRepository dispatchRepository;
    @InjectMocks private MobileDashboardServiceImpl mobileDashboardService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardUsesOnlyTheDriverLinkedToTheAuthenticatedJwtUser() {
        Driver driver = Driver.builder().id(17L).firstName("Asha").lastName("Mrema").status("AVAILABLE").build();
        User user = User.builder().email("asha@falconenergy.local").status(UserStatus.ACTIVE)
                .roleEntity(Role.builder().roleName("DRIVER").build()).driver(driver).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(), "token", List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(vehicleRepository.findByDriverId(17L)).thenReturn(Optional.of(Vehicle.builder()
                .id(23L).truckNumber("FAL-023").plateNumber("T 123 ABC").currentStatus("ASSIGNED").build()));
        when(deliveryRepository.getMobileDashboardCounts(eq(17L), anyList(), any(), any(), any()))
                .thenReturn(new MobileDeliveryCounts(2L, 1L));
        when(dispatchRepository.countPendingMobileDeliveries(eq(17L), anyList())).thenReturn(3L);
        when(deliveryRepository.findRecentForMobileDriver(eq(17L), any())).thenReturn(List.of());

        MobileDashboardResponse response = mobileDashboardService.getDashboard();

        assertEquals(17L, response.getDriver().getDriverId());
        assertEquals("Asha Mrema", response.getDriver().getDriverName());
        assertEquals("FAL-023", response.getDriver().getAssignedVehicle().getTruckNumber());
        assertEquals(5L, response.getSummary().getAssignedDeliveries());
        assertEquals(3L, response.getSummary().getPendingDeliveries());
        assertEquals(2L, response.getSummary().getDeliveriesInProgress());
        assertEquals(1L, response.getSummary().getCompletedToday());
        assertEquals(0L, response.getNotifications().getUnreadCount());
    }

    @Test
    void getDeliveriesReturnsDriverDeliveries() {
        Driver driver = Driver.builder().id(17L).firstName("Asha").lastName("Mrema").status("AVAILABLE").build();
        User user = User.builder().email("asha@falconenergy.local").status(UserStatus.ACTIVE)
                .roleEntity(Role.builder().roleName("DRIVER").build()).driver(driver).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.getEmail(), "token", List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(deliveryRepository.findRecentForMobileDriver(eq(17L), any())).thenReturn(List.of());

        List<MobileDashboardResponse.RecentDelivery> result = mobileDashboardService.getDeliveries();

        assertEquals(0, result.size());
    }
}
