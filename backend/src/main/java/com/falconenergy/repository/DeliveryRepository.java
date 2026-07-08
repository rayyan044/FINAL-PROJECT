package com.falconenergy.repository;

import com.falconenergy.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long>, JpaSpecificationExecutor<Delivery> {
    Optional<Delivery> findByDeliveryNumber(String deliveryNumber);
    boolean existsByDeliveryNumber(String deliveryNumber);
}
