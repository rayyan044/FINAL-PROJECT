package com.falconenergy.service.impl;

import com.falconenergy.dto.AdminDashboardResponse;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.UserStatus;
import com.falconenergy.repository.CustomerRepository;
import com.falconenergy.repository.DeliveryRepository;
import com.falconenergy.repository.FuelOrderRepository;
import com.falconenergy.repository.UserRepository;
import com.falconenergy.service.AdminDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final FuelOrderRepository fuelOrderRepository;
    private final DeliveryRepository deliveryRepository;

    public AdminDashboardServiceImpl(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            FuelOrderRepository fuelOrderRepository,
            DeliveryRepository deliveryRepository
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.fuelOrderRepository = fuelOrderRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public AdminDashboardResponse getAdminDashboardStats() {
        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .inactiveUsers(userRepository.countByStatus(UserStatus.INACTIVE))
                .customers(customerRepository.count())
                .fuelRequests(fuelOrderRepository.count())
                .deliveries(deliveryRepository.count())
                .salesOfficers(userRepository.countByRole(UserRole.SALES_OFFICER))
                .build();
    }
}
