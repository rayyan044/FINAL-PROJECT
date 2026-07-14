package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "truck_nominations")
@SQLDelete(sql = "UPDATE truck_nominations SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckNomination extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private FuelOrder order;

    @Column(name = "transport_source", nullable = false, length = 50)
    private String transportSource; // CUSTOMER_TRUCKS, FALCON_ARRANGED

    @Column(name = "number_of_trucks")
    private Integer numberOfTrucks;

    @Column(name = "confirmation_notes", columnDefinition = "TEXT")
    private String confirmationNotes;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, SUBMITTED

    @Column(name = "total_allocated_quantity", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAllocatedQuantity = BigDecimal.ZERO;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(mappedBy = "nomination", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TruckNominationItem> items = new ArrayList<>();
}
