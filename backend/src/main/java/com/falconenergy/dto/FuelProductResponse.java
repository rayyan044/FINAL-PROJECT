package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelProductResponse {
    private Long id;
    private String productName;
    private String fuelType;
    private BigDecimal unitPrice;
    private BigDecimal density;
    private BigDecimal availableQuantity;
    private String status;
    private String currency;
    private String fuelCategory;
    private String specification;
    private String description;
    private String unitOfMeasurement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
