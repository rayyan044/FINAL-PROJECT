package com.falconenergy.entity;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime;
@Entity @Table(name="payments") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment extends BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="invoice_id",nullable=false) private Invoice invoice;
 @Column(name="payment_reference",nullable=false,unique=true) private String paymentReference;
 @Column(nullable=false) private String gateway;
 @Column(name="payment_method",nullable=false) private String paymentMethod;
 @Column(name="phone_number") private String phoneNumber;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal amount;
 @Column(nullable=false,length=10) private String currency;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private PaymentStatus status;
 /** Locally generated transaction identifier used by development/demo payment simulation. */
 @Column(name="gateway_transaction_id",nullable=false,unique=true) private String gatewayTransactionId;
 @Column(name="failure_reason") private String failureReason;
 @Column(name="initiated_at") private LocalDateTime initiatedAt;
 @Column(name="completed_at") private LocalDateTime completedAt;
}
