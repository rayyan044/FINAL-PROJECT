package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_orders")
@SQLDelete(sql = "UPDATE fuel_orders SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@org.hibernate.annotations.DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private FuelProduct product;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "order_date", nullable = false)
    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    @Column(name = "order_status", nullable = false, length = 30)
    @Builder.Default
    private String orderStatus = "PENDING";

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "driver_phone", length = 50)
    private String driverPhone;

    @Column(name = "driver_email", length = 100)
    private String driverEmail;

    @Column(name = "location_gps", length = 100)
    private String locationGps;

    @Column(name = "location_address", columnDefinition = "TEXT")
    private String locationAddress;

    @Column(name = "location_landmark", columnDefinition = "TEXT")
    private String locationLandmark;



    @Column(name = "emergency_level", length = 50)
    private String emergencyLevel;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "original_quantity", precision = 12, scale = 2)
    private BigDecimal originalQuantity;

    @Column(name = "approved_quantity", precision = 12, scale = 2)
    private BigDecimal approvedQuantity;

    @Column(name = "edit_reason")
    private String editReason;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "levies", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal levies = BigDecimal.ZERO;

    @Column(name = "discount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "transport_charges", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal transportCharges = BigDecimal.ZERO;

    @Column(name = "delivery_charges", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal deliveryCharges = BigDecimal.ZERO;

    @Column(name = "additional_charges", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal additionalCharges = BigDecimal.ZERO;

    @Column(name = "delivery_method", length = 100)
    private String deliveryMethod;

    @Column(name = "incoterms", length = 100)
    private String incoterms;

    @Column(name = "port", length = 100)
    private String port;

    @Column(name = "destination", length = 150)
    private String destination;

    @Column(name = "logistics_info", columnDefinition = "TEXT")
    private String logisticsInfo;

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Invoice invoice;

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private TruckNomination truckNomination;

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private LoadingOrder loadingOrder;
}
