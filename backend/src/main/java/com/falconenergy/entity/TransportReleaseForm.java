package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "transport_release_forms")
@SQLDelete(sql = "UPDATE transport_release_forms SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransportReleaseForm extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "release_form_number", nullable = false, unique = true) private String releaseFormNumber;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "loading_activity_id", nullable = false, unique = true) private LoadingActivity loadingActivity;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "loading_report_id", nullable = false) private LoadingReport loadingReport;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "delivery_note_id", nullable = false) private DeliveryNote deliveryNote;
    @Column(name = "release_status", nullable = false) private String releaseStatus;
    @Column(name = "prepared_at", nullable = false) private LocalDateTime preparedAt;
    @Column(name = "prepared_by") private String preparedBy;
}
