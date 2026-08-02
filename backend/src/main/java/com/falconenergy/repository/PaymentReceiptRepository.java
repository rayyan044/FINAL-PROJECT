package com.falconenergy.repository;
import com.falconenergy.entity.PaymentReceipt;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {
    Optional<PaymentReceipt> findByInvoiceId(Long invoiceId);
    boolean existsByInvoiceId(Long invoiceId);
    @Query("SELECT MAX(p.receiptNumber) FROM PaymentReceipt p WHERE p.receiptNumber LIKE :prefix%") String findMaxReceiptNumberWithPrefix(@Param("prefix") String prefix);
}
