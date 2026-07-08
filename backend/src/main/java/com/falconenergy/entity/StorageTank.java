package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

@Entity
@Table(name = "storage_tanks")
@SQLDelete(sql = "UPDATE storage_tanks SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageTank extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tank_name", nullable = false, unique = true, length = 100)
    private String tankName;

    @Column(name = "capacity", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacity;

    @Column(name = "current_volume", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentVolume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuel_product_id", nullable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private FuelProduct fuelProduct;

    @Column(name = "location", length = 255)
    private String location;
}
