package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNoteResponse {
    private Long id;
    private String deliveryNoteNumber;
    private Long loadingOrderId;
    private Long loadingActivityId;
    private Long loadingReportId;
    private Long customerId;
    private String customerName;
    private Long productId;
    private String productName;
    private String truckNumber;
    private String driverName;
    private String driverLicenseNumber;
    private String transportCompany;
    private BigDecimal truckCapacity;
    private BigDecimal transportCharge;
    private BigDecimal ambientVolume;
    private BigDecimal standardVolume;
    private String destination;
    private String status;
    private String preparedBy;
    private LocalDateTime preparedAt;
    private String printedBy;
    private LocalDateTime printedAt;
    private LocalDateTime createdAt;
    private Long deliveryId;
}
