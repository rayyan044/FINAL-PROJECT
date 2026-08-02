package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fuel_price_ranges")
@SQLDelete(sql = "UPDATE fuel_price_ranges SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelPriceRange extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuel_product_id", nullable = false)
    private FuelProduct fuelProduct;

    @Column(name = "min_litres", nullable = false, precision = 12, scale = 2)
    private BigDecimal minLitres;

    @Column(name = "max_litres", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxLitres;

    @Column(name = "price_per_litre", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerLitre;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
