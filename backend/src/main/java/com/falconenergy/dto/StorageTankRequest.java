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
public class StorageTankRequest {

    @NotBlank(message = "Tank name is required")
    @Size(max = 100, message = "Tank name cannot exceed 100 characters")
    private String tankName;

    @NotNull(message = "Capacity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Capacity must be positive")
    private BigDecimal capacity;

    @NotNull(message = "Current volume is required")
    @DecimalMin(value = "0.0", message = "Current volume cannot be negative")
    private BigDecimal currentVolume;

    @NotNull(message = "Fuel product ID is required")
    private Long fuelProductId;

    @Size(max = 255, message = "Location cannot exceed 255 characters")
    private String location;
}
