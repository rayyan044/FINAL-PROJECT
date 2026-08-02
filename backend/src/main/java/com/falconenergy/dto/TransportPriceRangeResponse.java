package com.falconenergy.dto;
import lombok.*; import java.math.BigDecimal; import java.time.*;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor public class TransportPriceRangeResponse { private Long id; private Long fuelProductId; private String fuelProductName; private BigDecimal minLitres; private BigDecimal maxLitres; private BigDecimal transportPrice; private LocalDate effectiveDate; private String status; private String createdBy; private LocalDateTime createdAt; private LocalDateTime updatedAt; }
