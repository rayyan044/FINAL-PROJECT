package com.falconenergy.service;

import com.falconenergy.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface ReportingService {
    DashboardSummaryResponse getDashboard();
    SalesReportResponse getSalesReport(LocalDate from, LocalDate to);
    InventoryReportResponse getInventoryReport();
    LoadingReportAnalyticsResponse getLoadingReport(LocalDate from, LocalDate to);
    DispatchReportResponse getDispatchReport(LocalDate from, LocalDate to);
    DeliveryReportResponse getDeliveryReport(LocalDate from, LocalDate to);
    CustomerReportResponse getCustomerReport(Long customerId);
    FinancialReportResponse getFinancialReport(LocalDate from, LocalDate to);
    List<ReportSnapshotResponse> getReportHistory();
}
