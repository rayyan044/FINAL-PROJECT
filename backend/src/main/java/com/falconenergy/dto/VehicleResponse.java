package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {
    private Long id;
    private String plateNumber;
    private String truckNumber;
    private BigDecimal capacity;
    private String currentStatus;
    private boolean active;
    private Set<String> assignedFuelTypes;
    private DriverResponse driver;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
