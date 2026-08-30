package com.falconenergy.dto;
import java.math.BigDecimal;
import lombok.*;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransportDistanceRateResponse { private Long id; private BigDecimal minimumKm; private BigDecimal maximumKm; private BigDecimal price; private boolean active; }
