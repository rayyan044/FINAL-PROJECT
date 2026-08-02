package com.falconenergy.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelPriceRangeResponse {
    private Long id;
    private Long fuelProductId;
    private String fuelProductName;
    private String fuelType;
    private BigDecimal minLitres;
    private BigDecimal maxLitres;
    private BigDecimal pricePerLitre;
    private LocalDate effectiveDate;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
