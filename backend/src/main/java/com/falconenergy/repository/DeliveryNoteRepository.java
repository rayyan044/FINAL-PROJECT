package com.falconenergy.repository;

import com.falconenergy.entity.DeliveryNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface DeliveryNoteRepository extends JpaRepository<DeliveryNote, Long> {
    Optional<DeliveryNote> findByLoadingActivityId(Long loadingActivityId);
    boolean existsByLoadingActivityId(Long loadingActivityId);
    Optional<DeliveryNote> findByDeliveryNoteNumber(String deliveryNoteNumber);
    Optional<DeliveryNote> findByIdAndCustomerId(Long id, Long customerId);
    List<DeliveryNote> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT MAX(d.deliveryNoteNumber) FROM DeliveryNote d WHERE d.deliveryNoteNumber LIKE :prefix%")
    String findMaxDeliveryNoteNumberWithPrefix(@Param("prefix") String prefix);
}
