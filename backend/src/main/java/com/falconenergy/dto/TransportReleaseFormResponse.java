package com.falconenergy.dto;
import lombok.*; import java.time.LocalDateTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransportReleaseFormResponse { private Long id; private String releaseFormNumber; private Long loadingActivityId; private String loadingReportNumber; private String deliveryNoteNumber; private String truckNumber; private String driverName; private String destination; private String releaseStatus; private LocalDateTime preparedAt; private String preparedBy; }
