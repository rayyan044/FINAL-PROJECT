package com.falconenergy.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TruckPricingRequest {
 @NotNull @DecimalMin(value="0.01") private BigDecimal capacity;
 @NotBlank private String fuelType;
 @NotNull @DecimalMin(value="0.00") private BigDecimal transportPrice;
 private boolean active = true;
}
