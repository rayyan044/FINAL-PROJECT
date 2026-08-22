package com.falconenergy.repository;

import com.falconenergy.entity.FuelOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface FuelOrderRepository extends JpaRepository<FuelOrder, Long>, JpaSpecificationExecutor<FuelOrder> {
    Optional<FuelOrder> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);
    List<FuelOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT product_id FROM fuel_orders WHERE id = :orderId", nativeQuery = true)
    Long findProductIdByOrderId(@org.springframework.data.repository.query.Param("orderId") Long orderId);
}
