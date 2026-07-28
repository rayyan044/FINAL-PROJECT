package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_notes")
@SQLDelete(sql = "UPDATE delivery_notes SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_note_number", nullable = false, unique = true, length = 50)
    private String deliveryNoteNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_order_id")
    private LoadingOrder loadingOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_activity_id")
    private LoadingActivity loadingActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_report_id")
    private LoadingReport loadingReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private FuelProduct product;

    @Column(name = "truck_number", length = 50)
    private String truckNumber;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "driver_license_number", length = 100)
    private String driverLicenseNumber;

    @Column(name = "transport_company", length = 150)
    private String transportCompany;

    @Column(name = "ambient_volume", precision = 12, scale = 2)
    private BigDecimal ambientVolume;

    @Column(name = "standard_volume", precision = 12, scale = 2)
    private BigDecimal standardVolume;

    @Column(name = "destination", length = 255)
    private String destination;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // PREPARED, PRINTED, HANDED_TO_DRIVER

    @Column(name = "prepared_by", length = 100)
    private String preparedBy;

    @Column(name = "prepared_at")
    private LocalDateTime preparedAt;

    @Column(name = "printed_by", length = 100)
    private String printedBy;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;
}
