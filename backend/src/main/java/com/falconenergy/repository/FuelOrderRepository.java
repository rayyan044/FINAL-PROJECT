package com.falconenergy.repository;

import com.falconenergy.entity.FuelOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuelOrderRepository extends JpaRepository<FuelOrder, Long>, JpaSpecificationExecutor<FuelOrder> {
    Optional<FuelOrder> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);
}
