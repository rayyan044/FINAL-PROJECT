package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryReportResponse {
    private Long activeDeliveries;
    private Long deliveredTrucks;
    private Long pendingDeliveries;
    private BigDecimal deliveredVolume;
    private Double averageDeliveryCompletionTime; // in minutes
    private LocalDate fromDate;
    private LocalDate toDate;
}
