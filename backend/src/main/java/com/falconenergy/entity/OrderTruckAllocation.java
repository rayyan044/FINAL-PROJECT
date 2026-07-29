package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

/** Immutable-at-confirmation fleet and price snapshot for a customer order. */
@Entity
@Table(name = "order_truck_allocations", uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "vehicle_id"}))
@SQLDelete(sql = "UPDATE order_truck_allocations SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderTruckAllocation extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false)
    private FuelOrder order;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "allocated_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal allocatedQuantity;
    @Column(name = "capacity_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacitySnapshot;
    @Column(name = "transport_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal transportPrice;
}
