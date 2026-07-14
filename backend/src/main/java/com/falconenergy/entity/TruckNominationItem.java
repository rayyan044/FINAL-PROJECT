package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

@Entity
@Table(name = "truck_nomination_items")
@SQLDelete(sql = "UPDATE truck_nomination_items SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckNominationItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nomination_id", nullable = false)
    private TruckNomination nomination;

    @Column(name = "truck_number", nullable = false, length = 50)
    private String truckNumber;

    @Column(name = "trailer_number", nullable = false, length = 50)
    private String trailerNumber;

    @Column(name = "driver_name", nullable = false, length = 150)
    private String driverName;

    @Column(name = "driver_licence_number", nullable = false, length = 50)
    private String driverLicenceNumber;

    @Column(name = "driver_passport", length = 100)
    private String driverPassport;

    @Column(name = "transport_company", nullable = false, length = 150)
    private String transportCompany;

    @Column(name = "destination", nullable = false, length = 150)
    private String destination;

    @Column(name = "truck_capacity", nullable = false, precision = 12, scale = 2)
    private BigDecimal truckCapacity;

    @Column(name = "allocated_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal allocatedQuantity;
}
