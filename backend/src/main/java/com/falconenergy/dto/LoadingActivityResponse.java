package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadingActivityResponse {
    private Long id;
    private String truckNumber;
    private String trailerNumber;
    private String driverName;
    private String driverLicenceNumber;
    private String driverPassport;
    private String transportCompany;
    private String destination;
    private String product;
    private BigDecimal allocatedQuantity;
    private String queueNumber;
    private String bayNumber;
    private String pumpNumber;
    private LocalDateTime loadingStartTime;
    private LocalDateTime loadingCompletionTime;
    private String loadingOfficer;
    private String status;

    private BigDecimal ambientVolume;
    private BigDecimal temperature;
    private BigDecimal density;
    private BigDecimal standardVolume;
    private BigDecimal meterStart;
    private BigDecimal meterEnd;
    private BigDecimal meterDifference;
    private String remarks;
    private Long completedById;
    private String completedByName;
    private LocalDateTime completedAt;

    private List<LoadingCompartmentResponse> compartments;
    private List<LoadingReportResponse> reports;
}
