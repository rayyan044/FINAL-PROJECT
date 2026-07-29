package com.falconenergy.repository;

import com.falconenergy.entity.OrderTruckAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderTruckAllocationRepository extends JpaRepository<OrderTruckAllocation, Long> {
    List<OrderTruckAllocation> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
}
