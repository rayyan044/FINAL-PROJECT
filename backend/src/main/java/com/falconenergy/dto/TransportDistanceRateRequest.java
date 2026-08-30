package com.falconenergy.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TransportDistanceRateRequest {
    @NotNull @DecimalMin("0.0") private BigDecimal minimumKm;
    @DecimalMin("0.0") private BigDecimal maximumKm;
    @NotNull @DecimalMin("0.0") private BigDecimal price;
    private boolean active = true;
}
