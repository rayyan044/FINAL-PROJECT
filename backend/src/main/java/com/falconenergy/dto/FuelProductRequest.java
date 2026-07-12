package com.falconenergy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
public class FuelProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name cannot exceed 100 characters")
    private String productName;

    @NotBlank(message = "Fuel type is required")
    @Size(max = 50, message = "Fuel type cannot exceed 50 characters")
    private String fuelType;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Unit price format is invalid")
    private BigDecimal unitPrice;

    @NotNull(message = "Density is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Density must be positive")
    @Digits(integer = 4, fraction = 4, message = "Density must have up to 4 integer and 4 decimal places")
    private BigDecimal density;

    @Builder.Default
    private BigDecimal availableQuantity = BigDecimal.ZERO;

    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private String currency = "USD";

    private String fuelCategory;
    private String specification;
    private String description;

    @Builder.Default
    private String unitOfMeasurement = "Litres";
}
