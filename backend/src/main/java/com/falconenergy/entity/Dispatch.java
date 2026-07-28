package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispatches")
@SQLDelete(sql = "UPDATE dispatches SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispatch_number", nullable = false, unique = true, length = 50)
    private String dispatchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_order_id")
    private LoadingOrder loadingOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_activity_id")
    private LoadingActivity loadingActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_note_id")
    private DeliveryNote deliveryNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_invoice_id")
    private TruckInvoice truckInvoice;

    @Column(name = "truck_number", length = 50)
    private String truckNumber;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "driver_license_number", length = 100)
    private String driverLicenseNumber;

    @Column(name = "transport_company", length = 150)
    private String transportCompany;

    @Column(name = "destination", length = 255)
    private String destination;

    @Column(name = "dispatch_officer", length = 150)
    private String dispatchOfficer;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "released_by", length = 150)
    private String releasedBy;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "dispatch_status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DispatchStatus dispatchStatus;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
