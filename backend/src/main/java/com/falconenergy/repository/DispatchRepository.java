package com.falconenergy.repository;

import com.falconenergy.entity.Dispatch;
import com.falconenergy.entity.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    Optional<Dispatch> findByLoadingActivityId(Long loadingActivityId);
    Optional<Dispatch> findByDispatchNumber(String dispatchNumber);
    List<Dispatch> findByDispatchStatus(DispatchStatus status);
    boolean existsByLoadingActivityId(Long loadingActivityId);

    @Query("""
            SELECT COUNT(d) FROM Dispatch d
            WHERE d.loadingActivity.vehicle.driver.id = :driverId
              AND d.dispatchStatus IN :statuses
            """)
    long countPendingMobileDeliveries(@Param("driverId") Long driverId, @Param("statuses") java.util.Collection<DispatchStatus> statuses);

    @Query("SELECT MAX(d.dispatchNumber) FROM Dispatch d WHERE d.dispatchNumber LIKE :prefix%")
    String findMaxDispatchNumberWithPrefix(@Param("prefix") String prefix);
}
