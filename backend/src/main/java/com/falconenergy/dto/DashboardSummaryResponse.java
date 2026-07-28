package com.falconenergy.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private Long totalFuelOrders;
    private BigDecimal totalLitresSold;
    private BigDecimal totalSalesAmount;
    private BigDecimal availableInventory;
    private Long loadingActivitiesCompleted;
    private Long trucksDispatched;
    private Long activeDeliveries;
    private Long completedDeliveries;
    private BigDecimal totalRevenue;
    private BigDecimal outstandingInvoices;
}
