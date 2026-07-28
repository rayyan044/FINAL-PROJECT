package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReportResponse {
    private Long customerId;
    private String customerName;
    private BigDecimal totalPurchasedVolume;
    private BigDecimal totalAmountPaid;
    private Long deliveredTrucks;
    private List<CustomerOrderSummary> customerOrderHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerOrderSummary {
        private Long orderId;
        private String orderNumber;
        private BigDecimal quantity;
        private BigDecimal amount;
        private String status;
        private LocalDateTime orderDate;
    }
}
