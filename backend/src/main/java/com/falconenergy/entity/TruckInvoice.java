package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "truck_invoices")
@SQLDelete(sql = "UPDATE truck_invoices SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

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
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private FuelProduct product;

    @Column(name = "truck_number", length = 50)
    private String truckNumber;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "transport_company", length = 150)
    private String transportCompany;

    @Column(name = "truck_capacity", precision = 12, scale = 2)
    private BigDecimal truckCapacity;

    @Column(name = "transport_charge", precision = 12, scale = 2)
    private BigDecimal transportCharge;

    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_status", nullable = false, length = 50)
    @Builder.Default
    private String paymentStatus = "PENDING_PAYMENT";

    @Column(name = "invoice_status", nullable = false, length = 50)
    private String invoiceStatus; // GENERATED, PRINTED
}
