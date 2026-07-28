package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

@Entity
@Table(name = "loading_compartments")
@SQLDelete(sql = "UPDATE loading_compartments SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingCompartment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_activity_id", nullable = false)
    private LoadingActivity loadingActivity;

    @Column(name = "compartment_number", nullable = false)
    private int compartmentNumber;

    @Column(name = "capacity", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private FuelProduct product;

    @Column(name = "product_name_snapshot", nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(name = "product_code_snapshot", length = 50)
    private String productCodeSnapshot;

    @Column(name = "ambient_volume", nullable = false, precision = 12, scale = 2)
    private BigDecimal ambientVolume;

    @Column(name = "temperature", nullable = false, precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "density", nullable = false, precision = 6, scale = 4)
    private BigDecimal density;

    @Column(name = "standard_volume", nullable = false, precision = 12, scale = 2)
    private BigDecimal standardVolume;

    @Column(name = "seal_number", nullable = false, length = 100)
    private String sealNumber;
}
