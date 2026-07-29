package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

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

    @Column(name = "truck_number", nullable = false, unique = true, length = 50)
    private String truckNumber;

    @Column(name = "capacity", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacity;

    @Column(name = "current_status", nullable = false, length = 20)
    @Builder.Default
    private String currentStatus = "AVAILABLE"; // AVAILABLE, ASSIGNED, MAINTENANCE, OUT_OF_SERVICE

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "vehicle_fuel_types", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "fuel_type", nullable = false, length = 50)
    @Builder.Default
    private Set<String> assignedFuelTypes = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", unique = true)
    private Driver driver;
}
