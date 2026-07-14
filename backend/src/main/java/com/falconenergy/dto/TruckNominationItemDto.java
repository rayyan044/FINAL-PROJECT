package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckNominationItemDto {
    private Long id;
    private String truckNumber;
    private String trailerNumber;
    private String driverName;
    private String driverLicenceNumber;
    private String driverPassport;
    private String transportCompany;
    private String destination;
    private BigDecimal truckCapacity;
    private BigDecimal allocatedQuantity;
}
