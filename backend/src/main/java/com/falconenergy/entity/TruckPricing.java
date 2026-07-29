package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "truck_pricing", uniqueConstraints = @UniqueConstraint(columnNames = {"capacity", "fuel_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TruckPricing extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal capacity;
    @Column(name = "fuel_type", nullable = false, length = 50)
    private String fuelType;
    @Column(name = "transport_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal transportPrice;
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
