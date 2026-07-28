package com.falconenergy.repository;

import com.falconenergy.entity.TruckInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TruckInvoiceRepository extends JpaRepository<TruckInvoice, Long> {
    Optional<TruckInvoice> findByLoadingActivityId(Long loadingActivityId);
    boolean existsByLoadingActivityId(Long loadingActivityId);
    Optional<TruckInvoice> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT MAX(t.invoiceNumber) FROM TruckInvoice t WHERE t.invoiceNumber LIKE :prefix%")
    String findMaxInvoiceNumberWithPrefix(@Param("prefix") String prefix);
}
