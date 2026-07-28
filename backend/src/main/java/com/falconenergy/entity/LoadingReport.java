package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "loading_reports")
@SQLDelete(sql = "UPDATE loading_reports SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_activity_id", nullable = false)
    private LoadingActivity loadingActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loading_order_id", nullable = false)
    private LoadingOrder loadingOrder;

    @Column(name = "report_number", nullable = false, unique = true, length = 50)
    private String reportNumber;

    @Column(name = "loading_officer", nullable = false, length = 150)
    private String loadingOfficer;

    @Column(name = "terminal", nullable = false, length = 150)
    private String terminal;

    @Column(name = "loading_bay", nullable = false, length = 50)
    private String loadingBay;

    @Column(name = "report_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private LoadingReportStatus reportStatus;
}
