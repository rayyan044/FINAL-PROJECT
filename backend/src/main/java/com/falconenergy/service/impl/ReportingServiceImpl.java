package com.falconenergy.service.impl;

import com.falconenergy.dto.*;
import com.falconenergy.entity.*;
import com.falconenergy.repository.*;
import com.falconenergy.service.ReportingService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportingServiceImpl implements ReportingService {

    private final FuelOrderRepository fuelOrderRepository;
    private final FuelProductRepository fuelProductRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final LoadingActivityRepository loadingActivityRepository;
    private final LoadingReportRepository loadingReportRepository;
    private final DispatchRepository dispatchRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;
    private final TruckInvoiceRepository truckInvoiceRepository;
    private final ReportSnapshotRepository reportSnapshotRepository;
    private final CustomerRepository customerRepository;

    public ReportingServiceImpl(
            FuelOrderRepository fuelOrderRepository,
            FuelProductRepository fuelProductRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            LoadingActivityRepository loadingActivityRepository,
            LoadingReportRepository loadingReportRepository,
            DispatchRepository dispatchRepository,
            DeliveryRepository deliveryRepository,
            InvoiceRepository invoiceRepository,
            TruckInvoiceRepository truckInvoiceRepository,
            ReportSnapshotRepository reportSnapshotRepository,
            CustomerRepository customerRepository) {
        this.fuelOrderRepository = fuelOrderRepository;
        this.fuelProductRepository = fuelProductRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.loadingActivityRepository = loadingActivityRepository;
        this.loadingReportRepository = loadingReportRepository;
        this.dispatchRepository = dispatchRepository;
        this.deliveryRepository = deliveryRepository;
        this.invoiceRepository = invoiceRepository;
        this.truckInvoiceRepository = truckInvoiceRepository;
        this.reportSnapshotRepository = reportSnapshotRepository;
        this.customerRepository = customerRepository;
    }

    private String getCurrentUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return "system";
    }

    private String generateReportNumber() {
        String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        String prefix = "RPT-" + dateStr + "-";
        long count = reportSnapshotRepository.findAll().stream()
                .filter(r -> r.getReportNumber().startsWith(prefix))
                .count();
        return String.format("%s%04d", prefix, count + 1);
    }

    private void saveSnapshot(ReportType type, Map<String, Object> params) {
        ReportSnapshot snapshot = ReportSnapshot.builder()
                .reportNumber(generateReportNumber())
                .reportType(type)
                .generatedBy(getCurrentUsername())
                .generatedAt(LocalDateTime.now())
                .parameters(params)
                .status(ReportStatus.GENERATED)
                .build();
        reportSnapshotRepository.save(snapshot);
    }

    @Override
    public DashboardSummaryResponse getDashboard() {
        List<FuelOrder> orders = fuelOrderRepository.findAll();
        List<FuelProduct> products = fuelProductRepository.findAll();
        List<LoadingActivity> activities = loadingActivityRepository.findAll();
        List<Dispatch> dispatches = dispatchRepository.findAll();
        List<Delivery> deliveries = deliveryRepository.findAll();
        List<Invoice> invoices = invoiceRepository.findAll();

        long totalOrders = orders.size();
        
        BigDecimal totalLitresSold = orders.stream()
                .filter(o -> !o.getOrderStatus().equals("CANCELLED") && !o.getOrderStatus().equals("REJECTED"))
                .map(o -> o.getQuantity() != null ? o.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSalesAmount = orders.stream()
                .filter(o -> !o.getOrderStatus().equals("CANCELLED") && !o.getOrderStatus().equals("REJECTED"))
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableInventory = products.stream()
                .map(p -> p.getAvailableQuantity() != null ? p.getAvailableQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long loadingCompleted = activities.stream()
                .filter(a -> a.getStatus() == LoadingActivityStatus.COMPLETED)
                .count();

        long dispatchedCount = dispatches.stream()
                .filter(d -> d.getDispatchStatus() == DispatchStatus.DISPATCHED || d.getDispatchStatus() == DispatchStatus.IN_TRANSIT)
                .count();

        long activeDeliveries = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.IN_TRANSIT || d.getDeliveryStatus() == DeliveryStatus.ARRIVED_AT_DESTINATION)
                .count();

        long completedDeliveries = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.DELIVERED)
                .count();

        BigDecimal totalRevenue = invoices.stream()
                .filter(i -> "PAID".equals(i.getPaymentStatus()))
                .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingInvoices = invoices.stream()
                .filter(i -> "PENDING_PAYMENT".equals(i.getPaymentStatus()))
                .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardSummaryResponse.builder()
                .totalFuelOrders(totalOrders)
                .totalLitresSold(totalLitresSold)
                .totalSalesAmount(totalSalesAmount)
                .availableInventory(availableInventory)
                .loadingActivitiesCompleted(loadingCompleted)
                .trucksDispatched(dispatchedCount)
                .activeDeliveries(activeDeliveries)
                .completedDeliveries(completedDeliveries)
                .totalRevenue(totalRevenue)
                .outstandingInvoices(outstandingInvoices)
                .build();
    }

    @Override
    public SalesReportResponse getSalesReport(LocalDate from, LocalDate to) {
        LocalDate finalFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate finalTo = to != null ? to : LocalDate.now();

        LocalDateTime start = finalFrom.atStartOfDay();
        LocalDateTime end = finalTo.plusDays(1).atStartOfDay();

        List<FuelOrder> orders = fuelOrderRepository.findAll().stream()
                .filter(o -> o.getOrderDate() != null && o.getOrderDate().isAfter(start) && o.getOrderDate().isBefore(end))
                .collect(Collectors.toList());

        long orderCount = orders.size();

        BigDecimal totalQty = orders.stream()
                .filter(o -> !o.getOrderStatus().equals("CANCELLED") && !o.getOrderStatus().equals("REJECTED"))
                .map(o -> o.getQuantity() != null ? o.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalValue = orders.stream()
                .filter(o -> !o.getOrderStatus().equals("CANCELLED") && !o.getOrderStatus().equals("REJECTED"))
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by product
        Map<String, List<FuelOrder>> ordersByProduct = orders.stream()
                .filter(o -> o.getProduct() != null)
                .collect(Collectors.groupingBy(o -> o.getProduct().getProductName()));

        List<SalesReportResponse.ProductSalesDetail> salesByProduct = ordersByProduct.entrySet().stream()
                .map(entry -> {
                    BigDecimal vol = entry.getValue().stream()
                            .filter(o -> !o.getOrderStatus().equals("CANCELLED") && !o.getOrderStatus().equals("REJECTED"))
                            .map(o -> o.getQuantity() != null ? o.getQuantity() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal amt = entry.getValue().stream()
                            .filter(o -> !o.getOrderStatus().equals("CANCELLED") && !o.getOrderStatus().equals("REJECTED"))
                            .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return SalesReportResponse.ProductSalesDetail.builder()
                            .productName(entry.getKey())
                            .volume(vol)
                            .amount(amt)
                            .build();
                }).collect(Collectors.toList());

        // Group by customer
        Map<String, List<FuelOrder>> ordersByCustomer = orders.stream()
                .filter(o -> o.getCustomer() != null)
                .collect(Collectors.groupingBy(o -> o.getCustomer().getCompanyName()));

        List<SalesReportResponse.CustomerSalesDetail> salesByCustomer = ordersByCustomer.entrySet().stream()
                .map(entry -> {
                    BigDecimal vol = entry.getValue().stream()
                            .filter(o -> !o.getOrderStatus().equals("CANCELLED") && !o.getOrderStatus().equals("REJECTED"))
                            .map(o -> o.getQuantity() != null ? o.getQuantity() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal amt = entry.getValue().stream()
                            .filter(o -> !o.getOrderStatus().equals("CANCELLED") && !o.getOrderStatus().equals("REJECTED"))
                            .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return SalesReportResponse.CustomerSalesDetail.builder()
                            .customerName(entry.getKey())
                            .volume(vol)
                            .amount(amt)
                            .build();
                }).collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("from", finalFrom.toString());
        params.put("to", finalTo.toString());
        saveSnapshot(ReportType.SALES_SUMMARY, params);

        return SalesReportResponse.builder()
                .numberOrders(orderCount)
                .totalQuantitySold(totalQty)
                .totalSalesValue(totalValue)
                .salesByProduct(salesByProduct)
                .salesByCustomer(salesByCustomer)
                .fromDate(finalFrom)
                .toDate(finalTo)
                .build();
    }

    @Override
    public InventoryReportResponse getInventoryReport() {
        List<FuelProduct> products = fuelProductRepository.findAll();
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findAll();

        BigDecimal currentStock = products.stream()
                .map(p -> p.getAvailableQuantity() != null ? p.getAvailableQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal loadingDeductions = transactions.stream()
                .filter(t -> t.getMovementType() == InventoryMovementType.LOADING)
                .map(t -> t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal adjustments = transactions.stream()
                .filter(t -> t.getMovementType() == InventoryMovementType.ADJUSTMENT)
                .map(t -> t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InventoryReportResponse.StockMovementDetail> history = transactions.stream()
                .sorted(Comparator.comparing(InventoryTransaction::getCreatedAt).reversed())
                .map(t -> InventoryReportResponse.StockMovementDetail.builder()
                        .productName(t.getProduct() != null ? t.getProduct().getProductName() : "Unknown")
                        .transactionType(t.getMovementType() != null ? t.getMovementType().name() : "UNKNOWN")
                        .quantity(t.getQuantity())
                        .referenceNumber(t.getReferenceId() != null ? t.getReferenceType() + "-" + t.getReferenceId() : "N/A")
                        .timestamp(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<InventoryReportResponse.ProductStockDetail> details = products.stream()
                .map(p -> {
                    BigDecimal loaded = transactions.stream()
                            .filter(t -> t.getProduct() != null && t.getProduct().getId().equals(p.getId()) && t.getMovementType() == InventoryMovementType.LOADING)
                            .map(t -> t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal balance = p.getAvailableQuantity() != null ? p.getAvailableQuantity() : BigDecimal.ZERO;
                    BigDecimal receipts = transactions.stream()
                            .filter(t -> t.getProduct() != null && t.getProduct().getId().equals(p.getId()) && t.getMovementType() == InventoryMovementType.RECEIPT)
                            .map(t -> t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal opening = balance.add(loaded).subtract(receipts);

                    return InventoryReportResponse.ProductStockDetail.builder()
                            .productName(p.getProductName())
                            .openingQuantity(opening)
                            .loadedQuantity(loaded)
                            .currentBalance(balance)
                            .build();
                }).collect(Collectors.toList());

        saveSnapshot(ReportType.INVENTORY_REPORT, new HashMap<>());

        return InventoryReportResponse.builder()
                .currentStock(currentStock)
                .loadingDeductions(loadingDeductions)
                .adjustments(adjustments)
                .stockMovementHistory(history)
                .remainingQuantityByProduct(details)
                .build();
    }

    @Override
    public LoadingReportAnalyticsResponse getLoadingReport(LocalDate from, LocalDate to) {
        LocalDate finalFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate finalTo = to != null ? to : LocalDate.now();

        LocalDateTime start = finalFrom.atStartOfDay();
        LocalDateTime end = finalTo.plusDays(1).atStartOfDay();

        List<LoadingActivity> activities = loadingActivityRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(start) && a.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());

        long totalActivities = activities.size();
        long completedActivities = activities.stream()
                .filter(a -> a.getStatus() == LoadingActivityStatus.COMPLETED)
                .count();

        BigDecimal ambientVol = activities.stream()
                .filter(a -> a.getStatus() == LoadingActivityStatus.COMPLETED)
                .map(a -> a.getAmbientVolume() != null ? a.getAmbientVolume() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal standardVol = activities.stream()
                .filter(a -> a.getStatus() == LoadingActivityStatus.COMPLETED)
                .map(a -> a.getStandardVolume() != null ? a.getStandardVolume() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgDuration = 0.0;
        List<LoadingActivity> completedWithTimes = activities.stream()
                .filter(a -> a.getStatus() == LoadingActivityStatus.COMPLETED && a.getLoadingStartTime() != null && a.getLoadingCompletionTime() != null)
                .collect(Collectors.toList());

        if (!completedWithTimes.isEmpty()) {
            double totalMinutes = completedWithTimes.stream()
                    .mapToDouble(a -> Duration.between(a.getLoadingStartTime(), a.getLoadingCompletionTime()).toMinutes())
                    .sum();
            avgDuration = totalMinutes / completedWithTimes.size();
        }

        double completionPercentage = totalActivities > 0 ? ((double) completedActivities / totalActivities) * 100.0 : 0.0;

        Map<String, Object> params = new HashMap<>();
        params.put("from", finalFrom.toString());
        params.put("to", finalTo.toString());
        saveSnapshot(ReportType.LOADING_REPORT, params);

        return LoadingReportAnalyticsResponse.builder()
                .totalTrucksLoaded(completedActivities)
                .totalAmbientVolume(ambientVol)
                .totalStandardVolume(standardVol)
                .averageLoadingDuration(avgDuration)
                .loadingCompletionPercentage(completionPercentage)
                .fromDate(finalFrom)
                .toDate(finalTo)
                .build();
    }

    @Override
    public DispatchReportResponse getDispatchReport(LocalDate from, LocalDate to) {
        LocalDate finalFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate finalTo = to != null ? to : LocalDate.now();

        LocalDateTime start = finalFrom.atStartOfDay();
        LocalDateTime end = finalTo.plusDays(1).atStartOfDay();

        List<Dispatch> dispatches = dispatchRepository.findAll().stream()
                .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().isAfter(start) && d.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());

        long totalDispatched = dispatches.size();

        Map<String, Long> statusMap = dispatches.stream()
                .collect(Collectors.groupingBy(d -> d.getDispatchStatus() != null ? d.getDispatchStatus().name() : "UNKNOWN", Collectors.counting()));

        List<DispatchReportResponse.StatusCount> statusDistribution = statusMap.entrySet().stream()
                .map(entry -> DispatchReportResponse.StatusCount.builder()
                        .status(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        Map<LocalDate, Long> dailyMap = dispatches.stream()
                .collect(Collectors.groupingBy(d -> d.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<DispatchReportResponse.DailyCount> dailyCount = dailyMap.entrySet().stream()
                .map(entry -> DispatchReportResponse.DailyCount.builder()
                        .date(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(DispatchReportResponse.DailyCount::getDate))
                .collect(Collectors.toList());

        long delays = dispatches.stream()
                .filter(d -> d.getRemarks() != null && d.getRemarks().toLowerCase().contains("delay"))
                .count();

        Map<String, Object> params = new HashMap<>();
        params.put("from", finalFrom.toString());
        params.put("to", finalTo.toString());
        saveSnapshot(ReportType.DISPATCH_REPORT, params);

        return DispatchReportResponse.builder()
                .totalDispatchedTrucks(totalDispatched)
                .dispatchStatusDistribution(statusDistribution)
                .dailyDispatchCount(dailyCount)
                .dispatchDelays(delays)
                .fromDate(finalFrom)
                .toDate(finalTo)
                .build();
    }

    @Override
    public DeliveryReportResponse getDeliveryReport(LocalDate from, LocalDate to) {
        LocalDate finalFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate finalTo = to != null ? to : LocalDate.now();

        LocalDateTime start = finalFrom.atStartOfDay();
        LocalDateTime end = finalTo.plusDays(1).atStartOfDay();

        List<Delivery> deliveries = deliveryRepository.findAll().stream()
                .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().isAfter(start) && d.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());

        long active = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.IN_TRANSIT || d.getDeliveryStatus() == DeliveryStatus.ARRIVED_AT_DESTINATION)
                .count();

        long delivered = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.DELIVERED)
                .count();

        long pending = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.ARRIVED_AT_DESTINATION) // Or count ready dispatches
                .count();

        BigDecimal deliveredVolume = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.DELIVERED && d.getLoadingActivity() != null)
                .map(d -> d.getLoadingActivity().getStandardVolume() != null ? d.getLoadingActivity().getStandardVolume() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgTime = 0.0;
        List<Delivery> completedWithTimes = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.DELIVERED && d.getDispatchedAt() != null && d.getDeliveredAt() != null)
                .collect(Collectors.toList());

        if (!completedWithTimes.isEmpty()) {
            double totalMinutes = completedWithTimes.stream()
                    .mapToDouble(d -> Duration.between(d.getDispatchedAt(), d.getDeliveredAt()).toMinutes())
                    .sum();
            avgTime = totalMinutes / completedWithTimes.size();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("from", finalFrom.toString());
        params.put("to", finalTo.toString());
        saveSnapshot(ReportType.DELIVERY_REPORT, params);

        return DeliveryReportResponse.builder()
                .activeDeliveries(active)
                .deliveredTrucks(delivered)
                .pendingDeliveries(pending)
                .deliveredVolume(deliveredVolume)
                .averageDeliveryCompletionTime(avgTime)
                .fromDate(finalFrom)
                .toDate(finalTo)
                .build();
    }

    @Override
    public CustomerReportResponse getCustomerReport(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + customerId));

        List<FuelOrder> orders = fuelOrderRepository.findAll().stream()
                .filter(o -> o.getCustomer() != null && o.getCustomer().getId().equals(customerId))
                .collect(Collectors.toList());

        BigDecimal totalPurchasedVolume = orders.stream()
                .filter(o -> !o.getOrderStatus().equals("CANCELLED") && !o.getOrderStatus().equals("REJECTED"))
                .map(o -> o.getQuantity() != null ? o.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sum amount paid from paid primary invoices
        BigDecimal totalAmountPaid = invoiceRepository.findAll().stream()
                .filter(i -> i.getOrder() != null && i.getOrder().getCustomer() != null && i.getOrder().getCustomer().getId().equals(customerId))
                .filter(i -> "PAID".equals(i.getPaymentStatus()))
                .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sum amount paid from paid truck invoices
        BigDecimal truckInvoicesPaid = truckInvoiceRepository.findAll().stream()
                .filter(ti -> ti.getCustomer() != null && ti.getCustomer().getId().equals(customerId))
                .filter(ti -> "PAID".equals(ti.getPaymentStatus()))
                .map(ti -> ti.getTotalAmount() != null ? ti.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalAmountPaid = totalAmountPaid.add(truckInvoicesPaid);

        long deliveredTrucksCount = deliveryRepository.findAll().stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.DELIVERED)
                .filter(d -> d.getLoadingOrder() != null && d.getLoadingOrder().getOrder() != null 
                        && d.getLoadingOrder().getOrder().getCustomer() != null
                        && d.getLoadingOrder().getOrder().getCustomer().getId().equals(customerId))
                .count();

        List<CustomerReportResponse.CustomerOrderSummary> orderHistory = orders.stream()
                .map(o -> CustomerReportResponse.CustomerOrderSummary.builder()
                        .orderId(o.getId())
                        .orderNumber(o.getOrderNumber())
                        .quantity(o.getQuantity())
                        .amount(o.getAmount())
                        .status(o.getOrderStatus())
                        .orderDate(o.getOrderDate())
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("customerId", customerId);
        saveSnapshot(ReportType.CUSTOMER_REPORT, params);

        return CustomerReportResponse.builder()
                .customerId(customerId)
                .customerName(customer.getCompanyName())
                .totalPurchasedVolume(totalPurchasedVolume)
                .totalAmountPaid(totalAmountPaid)
                .deliveredTrucks(deliveredTrucksCount)
                .customerOrderHistory(orderHistory)
                .build();
    }

    @Override
    public FinancialReportResponse getFinancialReport(LocalDate from, LocalDate to) {
        LocalDate finalFrom = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate finalTo = to != null ? to : LocalDate.now();

        LocalDateTime start = finalFrom.atStartOfDay();
        LocalDateTime end = finalTo.plusDays(1).atStartOfDay();

        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> i.getInvoiceDate() != null && i.getInvoiceDate().isAfter(start) && i.getInvoiceDate().isBefore(end))
                .collect(Collectors.toList());

        BigDecimal totalInvoicedAmount = invoices.stream()
                .filter(i -> !"CANCELLED".equals(i.getPaymentStatus()))
                .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidAmount = invoices.stream()
                .filter(i -> "PAID".equals(i.getPaymentStatus()))
                .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingAmount = invoices.stream()
                .filter(i -> "PENDING_PAYMENT".equals(i.getPaymentStatus()))
                .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group revenue by product name
        Map<String, List<Invoice>> invoicesByProduct = invoices.stream()
                .filter(i -> i.getOrder() != null && i.getOrder().getProduct() != null)
                .collect(Collectors.groupingBy(i -> i.getOrder().getProduct().getProductName()));

        List<FinancialReportResponse.ProductRevenueDetail> revenueByFuelProduct = invoicesByProduct.entrySet().stream()
                .map(entry -> {
                    BigDecimal rev = entry.getValue().stream()
                            .filter(i -> "PAID".equals(i.getPaymentStatus()))
                            .map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return FinancialReportResponse.ProductRevenueDetail.builder()
                            .productName(entry.getKey())
                            .revenue(rev)
                            .build();
                }).collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("from", finalFrom.toString());
        params.put("to", finalTo.toString());
        saveSnapshot(ReportType.FINANCIAL_REPORT, params);

        return FinancialReportResponse.builder()
                .totalInvoicedAmount(totalInvoicedAmount)
                .paidAmount(paidAmount)
                .outstandingAmount(outstandingAmount)
                .revenueByFuelProduct(revenueByFuelProduct)
                .fromDate(finalFrom)
                .toDate(finalTo)
                .build();
    }

    @Override
    public List<ReportSnapshotResponse> getReportHistory() {
        return reportSnapshotRepository.findAll().stream()
                .sorted(Comparator.comparing(ReportSnapshot::getGeneratedAt).reversed())
                .map(r -> ReportSnapshotResponse.builder()
                        .id(r.getId())
                        .reportNumber(r.getReportNumber())
                        .reportType(r.getReportType())
                        .generatedBy(r.getGeneratedBy())
                        .generatedAt(r.getGeneratedAt())
                        .parameters(r.getParameters())
                        .filePath(r.getFilePath())
                        .status(r.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
}
