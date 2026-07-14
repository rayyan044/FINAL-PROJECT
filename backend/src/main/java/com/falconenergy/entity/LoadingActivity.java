package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loading_activities")
@SQLDelete(sql = "UPDATE loading_activities SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_order_id", nullable = false)
    private LoadingOrder loadingOrder;

    @Column(name = "truck_number", nullable = false, length = 50)
    private String truckNumber;

    @Column(name = "trailer_number", length = 50)
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

    @Column(name = "product", nullable = false, length = 100)
    private String product;

    @Column(name = "allocated_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal allocatedQuantity;

    @Column(name = "queue_number", nullable = false, length = 50)
    private String queueNumber;

    @Column(name = "bay_number", nullable = false, length = 50)
    private String bayNumber;

    @Column(name = "pump_number", length = 50)
    private String pumpNumber;

    @Column(name = "loading_start_time")
    private LocalDateTime loadingStartTime;

    @Column(name = "loading_completion_time")
    private LocalDateTime loadingCompletionTime;

    @Column(name = "loading_officer", length = 100)
    private String loadingOfficer;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "WAITING"; // WAITING, LOADING, LOADED
}
