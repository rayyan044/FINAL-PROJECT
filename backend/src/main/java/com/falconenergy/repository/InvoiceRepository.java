package com.falconenergy.repository;

import com.falconenergy.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    boolean existsByInvoiceNumber(String invoiceNumber);
    boolean existsByOrderId(Long orderId);
    Optional<Invoice> findByOrderId(Long orderId);
    List<Invoice> findByOrderCustomerIdOrderByInvoiceDateDesc(Long customerId);
    Optional<Invoice> findByIdAndOrderCustomerId(Long id, Long customerId);

    @Query("SELECT i FROM Invoice i JOIN FETCH i.order o JOIN FETCH o.customer JOIN FETCH o.product WHERE i.id = :id")
    Optional<Invoice> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);
}
