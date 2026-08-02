package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_receipts")
@SQLDelete(sql = "UPDATE payment_receipts SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentReceipt extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "receipt_number", nullable = false, unique = true) private String receiptNumber;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id", nullable = false, unique = true) private Invoice invoice;
    @Column(name = "receipt_status", nullable = false) private String receiptStatus;
    @Column(name = "received_amount", nullable = false, precision = 14, scale = 2) private BigDecimal receivedAmount;
    @Column(name = "received_at", nullable = false) private LocalDateTime receivedAt;
    @Column(name = "confirmed_by") private String confirmedBy;
}
