package com.falconenergy.dto;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TruckPricingResponse { private Long id; private BigDecimal capacity; private String fuelType; private BigDecimal transportPrice; private boolean active; }
