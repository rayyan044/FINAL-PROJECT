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
 @Column(name="mobile_money_network",length=30) private String mobileMoneyNetwork;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal amount;
 @Column(nullable=false,length=10) private String currency;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private PaymentStatus status;
 /** Locally generated transaction identifier used by development/demo payment simulation. */
 @Column(name="gateway_transaction_id",nullable=false,unique=true) private String gatewayTransactionId;
 @Column(name="provider_reference",length=150) private String providerReference;
 @Column(name="failure_reason") private String failureReason;
 @Column(name="gateway_status",length=50) private String gatewayStatus;
 @Column(name="next_action",length=50) private String nextAction;
 @Column(name="authorization_url",columnDefinition="TEXT") private String authorizationUrl;
 @Column(name="authorization_instruction",columnDefinition="TEXT") private String authorizationInstruction;
 @Column(name="verified_at") private LocalDateTime verifiedAt;
 @Column(name="last_checked_at") private LocalDateTime lastCheckedAt;
 @Column(name="initiated_at") private LocalDateTime initiatedAt;
 @Column(name="completed_at") private LocalDateTime completedAt;
}
