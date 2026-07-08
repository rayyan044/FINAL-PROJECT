package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
@SQLDelete(sql = "UPDATE vehicles SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, unique = true, length = 30)
    private String plateNumber;

    @Column(name = "capacity", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacity;

    @Column(name = "current_status", nullable = false, length = 20)
    @Builder.Default
    private String currentStatus = "ACTIVE"; // ACTIVE, MAINTENANCE, INACTIVE

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", unique = true)
    private Driver driver;
}
