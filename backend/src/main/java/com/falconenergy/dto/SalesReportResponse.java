package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportResponse {
    private Long numberOrders;
    private BigDecimal totalQuantitySold;
    private BigDecimal totalSalesValue;
    private List<ProductSalesDetail> salesByProduct;
    private List<CustomerSalesDetail> salesByCustomer;
    private LocalDate fromDate;
    private LocalDate toDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSalesDetail {
        private String productName;
        private BigDecimal volume;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSalesDetail {
        private String customerName;
        private BigDecimal volume;
        private BigDecimal amount;
    }
}
