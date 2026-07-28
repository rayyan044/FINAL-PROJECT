package com.falconenergy.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchResponse {
    private Long id;
    private String dispatchNumber;
    private Long loadingOrderId;
    private String loadingOrderNumber;
    private Long loadingActivityId;
    private Long deliveryNoteId;
    private String deliveryNoteNumber;
    private Long truckInvoiceId;
    private String truckInvoiceNumber;
    private String truckNumber;
    private String driverName;
    private String driverLicenseNumber;
    private String transportCompany;
    private String destination;
    private String dispatchOfficer;
    private LocalDateTime departureTime;
    private String releasedBy;
    private LocalDateTime releasedAt;
    private String dispatchStatus;
    private String remarks;
    private LocalDateTime createdAt;
}
