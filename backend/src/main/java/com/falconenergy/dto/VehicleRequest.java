package com.falconenergy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequest {

    @NotBlank(message = "Plate number is required")
    @Size(max = 30, message = "Plate number cannot exceed 30 characters")
    private String plateNumber;

    @NotNull(message = "Capacity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Capacity must be positive")
    private BigDecimal capacity;

    @Builder.Default
    private String currentStatus = "ACTIVE"; // ACTIVE, MAINTENANCE, INACTIVE

    private Long driverId;
}
