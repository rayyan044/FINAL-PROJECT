package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_transactions")
@SQLDelete(sql = "UPDATE fuel_transactions SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_number", nullable = false, unique = true, length = 50)
    private String transactionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private FuelProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tank_id")
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private StorageTank storageTank;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType; // Purchase, Sale, Transfer, Adjustment

    @Column(name = "transaction_date", nullable = false)
    @Builder.Default
    private LocalDateTime transactionDate = LocalDateTime.now();
}
