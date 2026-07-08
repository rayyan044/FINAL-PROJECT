package com.falconenergy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Builder.Default
    private BigDecimal openingStock = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal receivedStock = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal issuedStock = BigDecimal.ZERO;

    @Builder.Default
    private LocalDate recordDate = LocalDate.now();
}
