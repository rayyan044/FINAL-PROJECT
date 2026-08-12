package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@SQLDelete(sql = "UPDATE deliveries SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_number", nullable = false, unique = true, length = 50)
    private String deliveryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id")
    private Dispatch dispatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_order_id")
    private LoadingOrder loadingOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_activity_id")
    private LoadingActivity loadingActivity;

    @OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
    private DeliveryNote deliveryNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_invoice_id")
    private TruckInvoice truckInvoice;

    @Column(name = "truck_number", length = 50)
    private String truckNumber;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "transport_company", length = 150)
    private String transportCompany;

    @Column(name = "destination", length = 255)
    private String destination;

    @Column(name = "delivery_status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "received_by", length = 150)
    private String receivedBy;

    @Column(name = "completed_by", length = 150)
    private String completedBy;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "start_latitude")
    private Double startLatitude;

    @Column(name = "start_longitude")
    private Double startLongitude;

    @Column(name = "pod_latitude")
    private Double podLatitude;

    @Column(name = "pod_longitude")
    private Double podLongitude;

    @Column(name = "pod_photo_path", length = 255)
    private String podPhotoPath;

    @Column(name = "pod_notes", columnDefinition = "TEXT")
    private String podNotes;

    @Column(name = "pod_uploaded_at")
    private LocalDateTime podUploadedAt;
}
