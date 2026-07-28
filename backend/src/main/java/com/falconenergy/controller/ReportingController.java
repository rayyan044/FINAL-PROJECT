package com.falconenergy.controller;

import com.falconenergy.dto.*;
import com.falconenergy.service.ReportingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/reports", "/api/reports"})
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_OFFICER', 'FINANCE', 'OPERATIONS', 'OPERATOR')")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard summary retrieved successfully",
                reportingService.getDashboard()
        ));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_OFFICER')")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sales report generated successfully",
                reportingService.getSalesReport(from, to)
        ));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<InventoryReportResponse>> getInventoryReport() {
        return ResponseEntity.ok(ApiResponse.success(
                "Inventory report generated successfully",
                reportingService.getInventoryReport()
        ));
    }

    @GetMapping("/loading")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS', 'OPERATOR')")
    public ResponseEntity<ApiResponse<LoadingReportAnalyticsResponse>> getLoadingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Loading report generated successfully",
                reportingService.getLoadingReport(from, to)
        ));
    }

    @GetMapping("/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<DispatchReportResponse>> getDispatchReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dispatch report generated successfully",
                reportingService.getDispatchReport(from, to)
        ));
    }

    @GetMapping("/delivery")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATIONS')")
    public ResponseEntity<ApiResponse<DeliveryReportResponse>> getDeliveryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Delivery report generated successfully",
                reportingService.getDeliveryReport(from, to)
        ));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES_OFFICER')")
    public ResponseEntity<ApiResponse<CustomerReportResponse>> getCustomerReport(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Customer history report generated successfully",
                reportingService.getCustomerReport(customerId)
        ));
    }

    @GetMapping("/financial")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> getFinancialReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Financial report generated successfully",
                reportingService.getFinancialReport(from, to)
        ));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ReportSnapshotResponse>>> getReportHistory() {
        return ResponseEntity.ok(ApiResponse.success(
                "Report snapshots history log retrieved successfully",
                reportingService.getReportHistory()
        ));
    }
}
