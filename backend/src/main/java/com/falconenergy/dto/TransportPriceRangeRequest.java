package com.falconenergy.dto;
import jakarta.validation.constraints.*; import lombok.*; import java.math.BigDecimal; import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor public class TransportPriceRangeRequest { @NotNull private Long fuelProductId; @NotNull @DecimalMin("0.01") private BigDecimal minLitres; @NotNull @DecimalMin("0.01") private BigDecimal maxLitres; @NotNull @DecimalMin("0.00") private BigDecimal transportPrice; @NotNull private LocalDate effectiveDate; private String status="ACTIVE"; }
