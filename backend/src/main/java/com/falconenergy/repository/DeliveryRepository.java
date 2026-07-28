package com.falconenergy.repository;

import com.falconenergy.entity.Delivery;
import com.falconenergy.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long>, JpaSpecificationExecutor<Delivery> {
    Optional<Delivery> findByDeliveryNumber(String deliveryNumber);
    boolean existsByDeliveryNumber(String deliveryNumber);
    Optional<Delivery> findByDispatchId(Long dispatchId);
    Optional<Delivery> findByLoadingActivityId(Long loadingActivityId);
    List<Delivery> findByDeliveryStatus(DeliveryStatus status);
    boolean existsByDispatchId(Long dispatchId);

    @Query("SELECT MAX(d.deliveryNumber) FROM Delivery d WHERE d.deliveryNumber LIKE :prefix%")
    String findMaxDeliveryNumberWithPrefix(@Param("prefix") String prefix);
}
