package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReportResponse {
    private BigDecimal currentStock;
    private BigDecimal loadingDeductions;
    private BigDecimal adjustments;
    private List<StockMovementDetail> stockMovementHistory;
    private List<ProductStockDetail> remainingQuantityByProduct;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMovementDetail {
        private String productName;
        private String transactionType;
        private BigDecimal quantity;
        private String referenceNumber;
        private java.time.LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStockDetail {
        private String productName;
        private BigDecimal openingQuantity;
        private BigDecimal loadedQuantity;
        private BigDecimal currentBalance;
    }
}
