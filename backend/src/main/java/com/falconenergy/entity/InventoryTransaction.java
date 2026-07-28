package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory_transactions")
@SQLDelete(sql = "UPDATE inventory_transactions SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private FuelProduct product;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "stock_before", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockBefore;

    @Column(name = "stock_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockAfter;

    @Column(name = "movement_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private InventoryMovementType movementType;

    @Column(name = "movement_reason", length = 255)
    private String movementReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;
}
