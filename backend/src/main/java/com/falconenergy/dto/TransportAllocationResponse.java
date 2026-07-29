package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransportAllocationResponse {
    private Long vehicleId;
    private String truckNumber;
    private String plateNumber;
    private BigDecimal capacity;
    private String driverName;
    private String transportCompany;
    private BigDecimal allocatedQuantity;
    private BigDecimal transportPrice;
}
