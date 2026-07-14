package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
