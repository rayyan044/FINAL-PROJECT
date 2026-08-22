package com.falconenergy.repository;

import com.falconenergy.entity.Delivery;
import com.falconenergy.entity.DeliveryStatus;
import com.falconenergy.repository.projection.MobileDeliveryCounts;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT d FROM Delivery d JOIN d.loadingOrder loadingOrder JOIN loadingOrder.order fuelOrder
            WHERE fuelOrder.customer.id = :customerId ORDER BY d.createdAt DESC
            """)
    List<Delivery> findForCustomer(@Param("customerId") Long customerId);

    @Query("""
            SELECT d FROM Delivery d JOIN d.loadingOrder loadingOrder JOIN loadingOrder.order fuelOrder
            WHERE d.id = :deliveryId AND fuelOrder.customer.id = :customerId
            """)
    Optional<Delivery> findForCustomerById(@Param("deliveryId") Long deliveryId, @Param("customerId") Long customerId);

    @Query("""
            SELECT new com.falconenergy.repository.projection.MobileDeliveryCounts(
                COALESCE(SUM(CASE WHEN d.deliveryStatus IN :inProgressStatuses THEN 1L ELSE 0L END), 0L),
                COALESCE(SUM(CASE WHEN d.deliveryStatus = :deliveredStatus
                    AND d.deliveredAt >= :todayStart AND d.deliveredAt < :tomorrowStart THEN 1L ELSE 0L END), 0L)
            )
            FROM Delivery d
            WHERE d.loadingActivity.vehicle.driver.id = :driverId
            """)
    MobileDeliveryCounts getMobileDashboardCounts(
            @Param("driverId") Long driverId,
            @Param("inProgressStatuses") List<DeliveryStatus> inProgressStatuses,
            @Param("deliveredStatus") DeliveryStatus deliveredStatus,
            @Param("todayStart") java.time.LocalDateTime todayStart,
            @Param("tomorrowStart") java.time.LocalDateTime tomorrowStart
    );

    @Query("""
            SELECT d FROM Delivery d
            JOIN FETCH d.loadingActivity activity
            LEFT JOIN FETCH d.deliveryNote note
            LEFT JOIN FETCH note.customer
            LEFT JOIN FETCH note.product
            LEFT JOIN FETCH d.loadingOrder loadingOrder
            LEFT JOIN FETCH loadingOrder.order fuelOrder
            LEFT JOIN FETCH fuelOrder.customer
            LEFT JOIN FETCH fuelOrder.product
            WHERE activity.vehicle.driver.id = :driverId
            ORDER BY d.createdAt DESC
            """)
    List<Delivery> findRecentForMobileDriver(@Param("driverId") Long driverId, Pageable pageable);

    @Query("""
            SELECT d FROM Delivery d
            JOIN FETCH d.loadingActivity activity
            LEFT JOIN FETCH d.deliveryNote note
            LEFT JOIN FETCH note.customer
            LEFT JOIN FETCH note.product
            LEFT JOIN FETCH d.loadingOrder loadingOrder
            LEFT JOIN FETCH loadingOrder.order fuelOrder
            LEFT JOIN FETCH fuelOrder.customer
            LEFT JOIN FETCH fuelOrder.product
            WHERE d.id = :deliveryId AND activity.vehicle.driver.id = :driverId
            """)
    Optional<Delivery> findForMobileDriver(@Param("deliveryId") Long deliveryId, @Param("driverId") Long driverId);

    @Query("SELECT MAX(d.deliveryNumber) FROM Delivery d WHERE d.deliveryNumber LIKE :prefix%")
    String findMaxDeliveryNumberWithPrefix(@Param("prefix") String prefix);
}
