package com.falconenergy.service;

import com.falconenergy.dto.*;
import com.falconenergy.entity.*;
import com.falconenergy.repository.*;
import com.falconenergy.service.impl.ReportingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportingServiceTest {

    @Mock
    private FuelOrderRepository fuelOrderRepository;
    @Mock
    private FuelProductRepository fuelProductRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private LoadingActivityRepository loadingActivityRepository;
    @Mock
    private LoadingReportRepository loadingReportRepository;
    @Mock
    private DispatchRepository dispatchRepository;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private TruckInvoiceRepository truckInvoiceRepository;
    @Mock
    private ReportSnapshotRepository reportSnapshotRepository;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ReportingServiceImpl reportingService;

    @BeforeEach
    void setUp() {
        // Mock security context leniently
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testDashboardCalculations() {
        // Arrange
        FuelProduct product = FuelProduct.builder().id(1L).productName("AGO").availableQuantity(new BigDecimal("10000.00")).build();
        FuelOrder order = FuelOrder.builder().id(1L).orderNumber("ORD-001").product(product).quantity(new BigDecimal("1000")).amount(new BigDecimal("1500")).orderStatus("APPROVED").orderDate(LocalDateTime.now()).build();
        LoadingActivity activity = LoadingActivity.builder().id(1L).status(LoadingActivityStatus.COMPLETED).build();
        Dispatch dispatch = Dispatch.builder().id(1L).dispatchStatus(DispatchStatus.DISPATCHED).build();
        Delivery delivery = Delivery.builder().id(1L).deliveryStatus(DeliveryStatus.DELIVERED).build();
        Invoice invoice = Invoice.builder().id(1L).grandTotal(new BigDecimal("1500")).paymentStatus("PAID").build();

        when(fuelOrderRepository.findAll()).thenReturn(Collections.singletonList(order));
        when(fuelProductRepository.findAll()).thenReturn(Collections.singletonList(product));
        when(loadingActivityRepository.findAll()).thenReturn(Collections.singletonList(activity));
        when(dispatchRepository.findAll()).thenReturn(Collections.singletonList(dispatch));
        when(deliveryRepository.findAll()).thenReturn(Collections.singletonList(delivery));
        when(invoiceRepository.findAll()).thenReturn(Collections.singletonList(invoice));

        // Act
        DashboardSummaryResponse response = reportingService.getDashboard();

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getTotalFuelOrders());
        assertEquals(new BigDecimal("1000"), response.getTotalLitresSold());
        assertEquals(new BigDecimal("1500"), response.getTotalSalesAmount());
        assertEquals(new BigDecimal("10000.00"), response.getAvailableInventory());
        assertEquals(1L, response.getLoadingActivitiesCompleted());
        assertEquals(1L, response.getTrucksDispatched());
        assertEquals(1L, response.getCompletedDeliveries());
        assertEquals(new BigDecimal("1500"), response.getTotalRevenue());
    }

    @Test
    void testInventoryReportMatchesTransactions() {
        // Arrange
        FuelProduct product = FuelProduct.builder().id(1L).productName("AGO").availableQuantity(new BigDecimal("50000")).build();
        InventoryTransaction transaction1 = InventoryTransaction.builder()
                .product(product)
                .quantity(new BigDecimal("20000"))
                .movementType(InventoryMovementType.LOADING)
                .stockBefore(new BigDecimal("70000"))
                .stockAfter(new BigDecimal("50000"))
                .build();
        transaction1.setCreatedAt(LocalDateTime.now());

        when(fuelProductRepository.findAll()).thenReturn(Collections.singletonList(product));
        when(inventoryTransactionRepository.findAll()).thenReturn(Collections.singletonList(transaction1));

        // Act
        InventoryReportResponse response = reportingService.getInventoryReport();

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("50000"), response.getCurrentStock());
        assertEquals(new BigDecimal("20000"), response.getLoadingDeductions());
        assertEquals(1, response.getStockMovementHistory().size());
        assertEquals("AGO", response.getStockMovementHistory().get(0).getProductName());
        verify(reportSnapshotRepository, times(1)).save(any(ReportSnapshot.class));
    }

    @Test
    void testLoadingReportCalculations() {
        // Arrange
        LoadingActivity activity = LoadingActivity.builder()
                .id(1L)
                .status(LoadingActivityStatus.COMPLETED)
                .ambientVolume(new BigDecimal("2000"))
                .standardVolume(new BigDecimal("1980"))
                .loadingStartTime(LocalDateTime.now().minusMinutes(30))
                .loadingCompletionTime(LocalDateTime.now())
                .build();
        activity.setCreatedAt(LocalDateTime.now());

        when(loadingActivityRepository.findAll()).thenReturn(Collections.singletonList(activity));

        // Act
        LoadingReportAnalyticsResponse response = reportingService.getLoadingReport(LocalDate.now().minusDays(1), LocalDate.now());

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getTotalTrucksLoaded());
        assertEquals(new BigDecimal("2000"), response.getTotalAmbientVolume());
        assertEquals(new BigDecimal("1980"), response.getTotalStandardVolume());
        assertEquals(30.0, response.getAverageLoadingDuration());
    }

    @Test
    void testDeliveryReportCalculations() {
        // Arrange
        LoadingActivity la = LoadingActivity.builder().standardVolume(new BigDecimal("5000")).build();
        Delivery delivery = Delivery.builder()
                .id(1L)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .loadingActivity(la)
                .dispatchedAt(LocalDateTime.now().minusHours(2))
                .deliveredAt(LocalDateTime.now())
                .build();
        delivery.setCreatedAt(LocalDateTime.now());

        when(deliveryRepository.findAll()).thenReturn(Collections.singletonList(delivery));

        // Act
        DeliveryReportResponse response = reportingService.getDeliveryReport(LocalDate.now().minusDays(1), LocalDate.now());

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getDeliveredTrucks());
        assertEquals(new BigDecimal("5000"), response.getDeliveredVolume());
        assertEquals(120.0, response.getAverageDeliveryCompletionTime());
    }

    @Test
    void testFinancialReportCalculations() {
        // Arrange
        FuelProduct product = FuelProduct.builder().productName("PMS").build();
        FuelOrder order = FuelOrder.builder().product(product).build();
        Invoice invoice = Invoice.builder()
                .grandTotal(new BigDecimal("5000"))
                .paymentStatus("PAID")
                .order(order)
                .invoiceDate(LocalDateTime.now())
                .build();

        when(invoiceRepository.findAll()).thenReturn(Collections.singletonList(invoice));

        // Act
        FinancialReportResponse response = reportingService.getFinancialReport(LocalDate.now().minusDays(1), LocalDate.now());

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("5000"), response.getTotalInvoicedAmount());
        assertEquals(new BigDecimal("5000"), response.getPaidAmount());
        assertEquals(BigDecimal.ZERO, response.getOutstandingAmount());
        assertEquals(1, response.getRevenueByFuelProduct().size());
        assertEquals("PMS", response.getRevenueByFuelProduct().get(0).getProductName());
        assertEquals(new BigDecimal("5000"), response.getRevenueByFuelProduct().get(0).getRevenue());
    }
}
