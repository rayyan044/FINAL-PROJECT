package com.falconenergy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequest {

    @NotBlank(message = "Plate number is required")
    @Size(max = 30, message = "Plate number cannot exceed 30 characters")
    private String plateNumber;

    @NotBlank(message = "Truck number is required")
    private String truckNumber;

    @NotNull(message = "Capacity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Capacity must be positive")
    private BigDecimal capacity;

    @Builder.Default
    private String currentStatus = "AVAILABLE";

    @Builder.Default
    private boolean active = true;

    @NotNull(message = "At least one assigned fuel type is required")
    private Set<@NotBlank String> assignedFuelTypes;

    private Long driverId;
}
