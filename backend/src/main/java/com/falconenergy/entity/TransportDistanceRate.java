package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

@Entity @Table(name = "transport_distance_rates")
@SQLDelete(sql = "UPDATE transport_distance_rates SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransportDistanceRate extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "minimum_km", nullable = false, precision = 10, scale = 3) private BigDecimal minimumKm;
    /** Null means this is the final, open-ended distance bracket. */
    @Column(name = "maximum_km", precision = 10, scale = 3) private BigDecimal maximumKm;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}
