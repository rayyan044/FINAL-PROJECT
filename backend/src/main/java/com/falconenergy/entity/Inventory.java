package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inventories")
@SQLDelete(sql = "UPDATE inventories SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private FuelProduct product;

    @Column(name = "opening_stock", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal openingStock = BigDecimal.ZERO;

    @Column(name = "received_stock", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal receivedStock = BigDecimal.ZERO;

    @Column(name = "issued_stock", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal issuedStock = BigDecimal.ZERO;

    @Column(name = "closing_stock", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal closingStock = BigDecimal.ZERO;

    @Column(name = "record_date", nullable = false)
    @Builder.Default
    private LocalDate recordDate = LocalDate.now();
}
