package com.falconenergy.repository;

import com.falconenergy.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
    boolean existsByUserIdAndDeliveryIdAndType(Long userId, Long deliveryId, String type);
    List<Notification> findByDeliveryIdAndUserIdNotAndIsReadFalse(Long deliveryId, Long userId);
}
