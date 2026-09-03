package com.falconenergy.repository;
import com.falconenergy.entity.Payment; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface PaymentRepository extends JpaRepository<Payment,Long> {
 Optional<Payment> findByPaymentReference(String reference);
 Optional<Payment> findByGatewayTransactionId(String transactionId);
 List<Payment> findByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);
 Optional<Payment> findFirstByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);
 List<Payment> findByInvoiceIdAndStatusInOrderByCreatedAtDesc(Long invoiceId, Collection<com.falconenergy.entity.PaymentStatus> statuses);
 Optional<Payment> findByIdAndInvoiceOrderCustomerId(Long id, Long customerId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Payment p where p.id=:id") Optional<Payment> findByIdForUpdate(@Param("id") Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Payment p where p.paymentReference=:reference") Optional<Payment> findByReferenceForUpdate(@Param("reference") String reference);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Payment p where p.gatewayTransactionId=:depositId") Optional<Payment> findByGatewayTransactionIdForUpdate(@Param("depositId") String depositId);
 List<Payment> findTop100ByGatewayAndStatusInOrderByUpdatedAtAsc(String gateway, Collection<com.falconenergy.entity.PaymentStatus> statuses);
}
