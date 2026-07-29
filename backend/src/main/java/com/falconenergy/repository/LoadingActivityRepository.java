package com.falconenergy.repository;

import com.falconenergy.entity.LoadingActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadingActivityRepository extends JpaRepository<LoadingActivity, Long> {
    java.util.List<LoadingActivity> findByLoadingOrderId(Long loadingOrderId);
    @org.springframework.data.jpa.repository.Query("select a from LoadingActivity a where a.vehicle.id = :vehicleId and a.status not in (com.falconenergy.entity.LoadingActivityStatus.DELIVERED, com.falconenergy.entity.LoadingActivityStatus.CANCELLED)")
    java.util.List<LoadingActivity> findActiveByVehicleId(@org.springframework.data.repository.query.Param("vehicleId") Long vehicleId);
}
