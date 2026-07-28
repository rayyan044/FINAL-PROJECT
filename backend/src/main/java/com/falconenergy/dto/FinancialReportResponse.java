package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReportResponse {
    private BigDecimal totalInvoicedAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private List<ProductRevenueDetail> revenueByFuelProduct;
    private LocalDate fromDate;
    private LocalDate toDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRevenueDetail {
        private String productName;
        private BigDecimal revenue;
    }
}
