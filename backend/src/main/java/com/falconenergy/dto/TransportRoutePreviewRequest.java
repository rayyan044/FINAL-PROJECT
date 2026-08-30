package com.falconenergy.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TransportRoutePreviewRequest { @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") private BigDecimal latitude; @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") private BigDecimal longitude; }
