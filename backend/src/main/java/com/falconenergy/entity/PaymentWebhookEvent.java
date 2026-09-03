package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "payment_webhook_events", uniqueConstraints = @UniqueConstraint(columnNames = {"gateway", "event_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentWebhookEvent {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(nullable = false, length = 30) private String gateway;
 @Column(name = "event_id", nullable = false, length = 150) private String eventId;
 @Column(name = "event_type", length = 100) private String eventType;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "payment_id") private Payment payment;
 @Column(name = "received_at", nullable = false) private LocalDateTime receivedAt;
}
