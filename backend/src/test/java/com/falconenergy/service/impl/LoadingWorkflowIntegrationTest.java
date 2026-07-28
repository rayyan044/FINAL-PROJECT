package com.falconenergy.service.impl;

import com.falconenergy.dto.*;
import com.falconenergy.entity.*;
import com.falconenergy.repository.*;
import com.falconenergy.service.LoadingOrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class LoadingWorkflowIntegrationTest {

    @Autowired
    private LoadingOrderService loadingOrderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    @Autowired
    private FuelProductRepository productRepository;

    @Autowired
    private LoadingOrderRepository loadingOrderRepository;

    @Autowired
    private LoadingActivityRepository loadingActivityRepository;

    @Autowired
    private LoadingReportRepository loadingReportRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Test
    @Transactional
    public void testEndToEndLoadingWorkflow() {
        // 1. Prepare Customer
        Customer customer = customerRepository.save(
                Customer.builder()
                        .companyName("Test Transit Co")
                        .customerCode("CUST-TEST-99")
                        .email("transit@test.com")
                        .contactPerson("Test Contact Person")
                        .status("ACTIVE")
                        .build()
        );

        // 2. Prepare Product with specific coefficient
        FuelProduct product = productRepository.save(
                FuelProduct.builder()
                        .productName("TEST AGO DIESEL")
                        .fuelType("AGO")
                        .unitPrice(new BigDecimal("1.20"))
                        .density(new BigDecimal("0.840"))
                        .availableQuantity(new BigDecimal("50000.00"))
                        .thermalExpansionCoefficient(new BigDecimal("0.00084"))
                        .status("ACTIVE")
                        .build()
        );

        // 3. Create Fuel Order
        FuelOrder fuelOrder = fuelOrderRepository.save(
                FuelOrder.builder()
                        .orderNumber("FO-TEST-E2E")
                        .customer(customer)
                        .product(product)
                        .quantity(new BigDecimal("20000.00"))
                        .originalQuantity(new BigDecimal("20000.00"))
                        .approvedQuantity(new BigDecimal("20000.00"))
                        .amount(new BigDecimal("24000.00"))
                        .orderStatus("LOADING_ORDER_CREATED")
                        .build()
        );

        // 4. Create Loading Order linked to Fuel Order
        LoadingOrder order = LoadingOrder.builder()
                .loadingOrderNumber("LO-TEST-E2E")
                .loadingTerminal("DAR ES SALAAM PORT")
                .consignee("Falcon Customer")
                .status(LoadingOrderStatus.APPROVED)
                .loadingDate(java.time.LocalDate.now())
                .order(fuelOrder)
                .build();

        LoadingActivity activity = LoadingActivity.builder()
                .loadingOrder(order)
                .truckNumber("T123-ABC")
                .driverName("John Doe")
                .driverLicenceNumber("DL-9999")
                .transportCompany("Falcon Transport")
                .destination("Malawi Depot")
                .product("TEST AGO DIESEL")
                .allocatedQuantity(new BigDecimal("20000.00"))
                .queueNumber("Q-001")
                .bayNumber("BAY-1")
                .status(LoadingActivityStatus.PENDING)
                .build();

        List<LoadingActivity> acts = new ArrayList<>();
        acts.add(activity);
        order.setActivities(acts);
        order = loadingOrderRepository.save(order);
        activity = order.getActivities().get(0);

        // 5. Transition 1: Start Loading Activity
        LoadingOrderResponse startedRes = loadingOrderService.startLoadingActivity(order.getId(), activity.getId(), "BAY-2", "PUMP-3");
        Assertions.assertEquals(LoadingOrderStatus.LOADING_IN_PROGRESS.name(), startedRes.getStatus());
        
        LoadingActivityResponse startedAct = startedRes.getActivities().get(0);
        Assertions.assertEquals(LoadingActivityStatus.STARTED, LoadingActivityStatus.valueOf(startedAct.getStatus()));
        Assertions.assertEquals("BAY-2", startedAct.getBayNumber());
        Assertions.assertEquals("PUMP-3", startedAct.getPumpNumber());

        // 6. Transition 2: Complete Loading with 2 Compartments
        LoadingCompartmentRequest comp1 = LoadingCompartmentRequest.builder()
                .compartmentNumber(1)
                .capacity(new BigDecimal("10000.00"))
                .productId(product.getId())
                .ambientVolume(new BigDecimal("10000.00"))
                .temperature(new BigDecimal("25.00")) // 5 degrees above 20
                .density(new BigDecimal("0.840"))
                .sealNumber("SEAL-001")
                .build();

        LoadingCompartmentRequest comp2 = LoadingCompartmentRequest.builder()
                .compartmentNumber(2)
                .capacity(new BigDecimal("10000.00"))
                .productId(product.getId())
                .ambientVolume(new BigDecimal("10000.00"))
                .temperature(new BigDecimal("20.00")) // standard temperature
                .density(new BigDecimal("0.840"))
                .sealNumber("SEAL-002")
                .build();

        LoadingActivityCompletionRequest completionReq = LoadingActivityCompletionRequest.builder()
                .bayNumber("BAY-2")
                .pumpNumber("PUMP-3")
                .meterStart(new BigDecimal("100000.00"))
                .meterEnd(new BigDecimal("120000.00")) // meter difference of 20000.00 equals sum of ambient volumes
                .remarks("E2E Loading completed successfully")
                .compartments(List.of(comp1, comp2))
                .build();

        LoadingOrderResponse completedRes = loadingOrderService.completeLoadingActivity(order.getId(), activity.getId(), completionReq);
        
        // 7. Assert: Loading Order should transition to DOCUMENTATION_PENDING when all activities are complete
        Assertions.assertEquals(LoadingOrderStatus.DOCUMENTATION_PENDING.name(), completedRes.getStatus());

        LoadingActivityResponse completedAct = completedRes.getActivities().get(0);
        Assertions.assertEquals(LoadingActivityStatus.COMPLETED, LoadingActivityStatus.valueOf(completedAct.getStatus()));

        // 8. Assert: Volumes calculations
        // Compartment 1 Standard volume = 10000 * [1 - 0.00084 * (25 - 20)] = 10000 * [1 - 0.0042] = 10000 * 0.9958 = 9958.00 L
        // Compartment 2 Standard volume = 10000 * [1 - 0.00084 * (20 - 20)] = 10000 L
        // Sum Standard Volume = 9958.00 + 10000.00 = 19958.00 L
        Assertions.assertEquals(0, completedAct.getAmbientVolume().compareTo(new BigDecimal("20000.00")));
        Assertions.assertEquals(0, completedAct.getStandardVolume().compareTo(new BigDecimal("19958.00")));

        // 9. Assert: Inventory Deduction (50000.00 - 19958.00 = 30042.00)
        FuelProduct afterProduct = productRepository.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(0, afterProduct.getAvailableQuantity().compareTo(new BigDecimal("30042.00")));

        // 10. Assert: Inventory transactions are logged with stock before/after
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findAll();
        Assertions.assertFalse(transactions.isEmpty());
        
        // Check that a transaction matches standard volume loaded from one of the compartments
        boolean hasCorrectTx = transactions.stream().anyMatch(t -> 
                t.getProduct().getId().equals(product.getId()) &&
                t.getQuantity().compareTo(new BigDecimal("9958.00")) == 0 &&
                t.getStockBefore().compareTo(new BigDecimal("50000.00")) == 0
        );
        Assertions.assertTrue(hasCorrectTx, "Inventory transaction log should track stock before and after correctly.");

        // 11. Assert: Report Generated
        LoadingReportResponse report = loadingOrderService.getLoadingReportByActivityId(activity.getId());
        Assertions.assertNotNull(report);
        Assertions.assertTrue(report.getReportNumber().startsWith("LR-"));
        Assertions.assertEquals(LoadingReportStatus.GENERATED.name(), report.getReportStatus());
    }
}
