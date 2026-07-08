package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private Long id;
    private FuelProductResponse product;
    private BigDecimal openingStock;
    private BigDecimal receivedStock;
    private BigDecimal issuedStock;
    private BigDecimal closingStock;
    private LocalDate recordDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
