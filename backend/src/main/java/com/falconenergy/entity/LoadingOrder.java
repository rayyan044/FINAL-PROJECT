package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loading_orders")
@SQLDelete(sql = "UPDATE loading_orders SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loading_order_number", nullable = false, unique = true, length = 50)
    private String loadingOrderNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private FuelOrder order;

    @Column(name = "loading_date", nullable = false)
    private LocalDate loadingDate;

    @Column(name = "loading_terminal", nullable = false, length = 150)
    private String loadingTerminal;

    @Column(name = "consignee", nullable = false, length = 150)
    private String consignee;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, APPROVED, LOADING_IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "prepared_by", length = 100)
    private String preparedBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "loading_remarks", columnDefinition = "TEXT")
    private String loadingRemarks;

    @Column(name = "vessel_name", length = 150)
    private String vesselName;

    @Column(name = "operations_manager", length = 200)
    private String operationsManager;

    @OneToMany(mappedBy = "loadingOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LoadingActivity> activities = new ArrayList<>();
}
