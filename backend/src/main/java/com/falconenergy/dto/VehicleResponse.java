package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {
    private Long id;
    private String plateNumber;
    private BigDecimal capacity;
    private String currentStatus;
    private DriverResponse driver;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
