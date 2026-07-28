package com.falconenergy.service.impl;

import com.falconenergy.dto.*;
import com.falconenergy.entity.*;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.repository.*;
import com.falconenergy.service.DeliveryDocumentService;
import com.falconenergy.service.DispatchService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class DispatchServiceTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private DeliveryDocumentService deliveryDocumentService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FuelProductRepository productRepository;

    @Autowired
    private FuelOrderRepository fuelOrderRepository;

    @Autowired
    private LoadingOrderRepository loadingOrderRepository;

    @Autowired
    private LoadingActivityRepository loadingActivityRepository;

    @Autowired
    private LoadingReportRepository loadingReportRepository;

    @Autowired
    private DeliveryNoteRepository deliveryNoteRepository;

    @Autowired
    private TruckInvoiceRepository truckInvoiceRepository;

    @Autowired
    private DispatchRepository dispatchRepository;

    @Test
    @Transactional
    @WithMockUser(username = "dispatch_officer", authorities = {"ROLE_DISPATCHER"})
    public void testDispatchManagementWorkflow() {
        // 1. Setup metadata
        Customer customer = customerRepository.save(Customer.builder()
                .companyName("Dispatch Test Company")
                .customerCode("CUST-DISP")
                .contactPerson("Bob Dispatcher")
                .email("bob@dispatch.com")
                .phone("9876543")
                .status("ACTIVE")
                .build());

        FuelProduct product = productRepository.save(FuelProduct.builder()
                .productName("DIS AGO")
                .fuelType("AGO")
                .unitPrice(new BigDecimal("1.60"))
                .density(new BigDecimal("0.8400"))
                .availableQuantity(new BigDecimal("100000.00"))
                .status("ACTIVE")
                .currency("USD")
                .build());

        FuelOrder order = fuelOrderRepository.save(FuelOrder.builder()
                .orderNumber("ORD-DISP-001")
                .customer(customer)
                .product(product)
                .quantity(new BigDecimal("15000.00"))
                .amount(new BigDecimal("24000.00"))
                .orderStatus("APPROVED")
                .build());

        LoadingOrder loadingOrder = loadingOrderRepository.save(LoadingOrder.builder()
                .loadingOrderNumber("LO-DISP-001")
                .order(order)
                .loadingDate(LocalDate.now())
                .loadingTerminal("Main Dispatch Terminal")
                .consignee("Dispatch Consignee")
                .status(LoadingOrderStatus.LOADING_IN_PROGRESS)
                .build());

        LoadingActivity activity = loadingActivityRepository.save(LoadingActivity.builder()
                .loadingOrder(loadingOrder)
                .truckNumber("T-DISP-222")
                .driverName("Sam Driver")
                .driverLicenceNumber("LIC-DISP-222")
                .transportCompany("Dispatch Carriers")
                .destination("Dispatch Yard")
                .product("AGO")
                .allocatedQuantity(new BigDecimal("15000.00"))
                .queueNumber("Q2")
                .bayNumber("BAY2")
                .status(LoadingActivityStatus.COMPLETED)
                .ambientVolume(new BigDecimal("15000.00"))
                .standardVolume(new BigDecimal("14900.00"))
                .completedAt(LocalDateTime.now())
                .build());

        // Assert: Cannot dispatch without loading report
        Assertions.assertThrows(BadRequestException.class, () -> {
            dispatchService.createDispatch(activity.getId(), null);
        });

        // 2. Create Loading Report
        LoadingReport report = loadingReportRepository.save(LoadingReport.builder()
                .loadingActivity(activity)
                .loadingOrder(loadingOrder)
                .reportNumber("REP-DISP-001")
                .loadingOfficer("dispatch_officer")
                .terminal("Main Dispatch Terminal")
                .loadingBay("BAY2")
                .reportStatus(LoadingReportStatus.GENERATED)
                .build());

        // Assert: Cannot dispatch without Delivery Note & Truck Invoice
        Assertions.assertThrows(BadRequestException.class, () -> {
            dispatchService.createDispatch(activity.getId(), null);
        });

        // 3. Create Delivery Note and Truck Invoice
        DeliveryNoteResponse dnResponse = deliveryDocumentService.generateDeliveryNote(activity.getId());
        TruckInvoiceResponse invoiceResponse = deliveryDocumentService.generateTruckInvoice(activity.getId());

        // Assert: Cannot dispatch if Delivery Note is not HANDED_TO_DRIVER
        Assertions.assertThrows(BadRequestException.class, () -> {
            dispatchService.createDispatch(activity.getId(), null);
        });

        // Print documents
        deliveryDocumentService.printDeliveryNote(dnResponse.getId());
        deliveryDocumentService.printTruckInvoice(invoiceResponse.getId());

        // Handover documents to driver
        deliveryDocumentService.markHandedToDriver(dnResponse.getId());

        // Assert: Ready truck now appears in pending dispatch queue
        List<LoadingActivityResponse> pendingQueue = dispatchService.getPendingDispatchActivities();
        Assertions.assertFalse(pendingQueue.isEmpty());
        Assertions.assertTrue(pendingQueue.stream().anyMatch(act -> act.getId().equals(activity.getId())));

        // 4. Create Dispatch Record
        DispatchResponse dispatchResponse = dispatchService.createDispatch(activity.getId(),
                DispatchRequest.builder().remarks("E2E Dispatch setup").build());

        Assertions.assertNotNull(dispatchResponse);
        Assertions.assertTrue(dispatchResponse.getDispatchNumber().startsWith("DIS-"));
        Assertions.assertEquals("READY", dispatchResponse.getDispatchStatus());
        Assertions.assertEquals("T-DISP-222", dispatchResponse.getTruckNumber());
        Assertions.assertEquals("Sam Driver", dispatchResponse.getDriverName());

        // Assert: Duplicate prevention
        Assertions.assertThrows(BadRequestException.class, () -> {
            dispatchService.createDispatch(activity.getId(), null);
        });

        // 5. Release Truck
        DispatchResponse releasedRes = dispatchService.releaseTruck(dispatchResponse.getId());
        Assertions.assertEquals("DISPATCHED", releasedRes.getDispatchStatus());
        Assertions.assertNotNull(releasedRes.getReleasedBy());
        Assertions.assertNotNull(releasedRes.getReleasedAt());
        Assertions.assertEquals("dispatch_officer", releasedRes.getReleasedBy());

        // Verify LoadingActivity status updated to DISPATCHED
        LoadingActivity updatedActivity = loadingActivityRepository.findById(activity.getId()).orElseThrow();
        Assertions.assertEquals(LoadingActivityStatus.DISPATCHED, updatedActivity.getStatus());

        // Verify LoadingOrder status updated to DISPATCHED
        LoadingOrder updatedOrder = loadingOrderRepository.findById(loadingOrder.getId()).orElseThrow();
        Assertions.assertEquals(LoadingOrderStatus.DISPATCHED, updatedOrder.getStatus());

        // 6. Start Transit
        DispatchResponse transitRes = dispatchService.startTransit(dispatchResponse.getId());
        Assertions.assertEquals("IN_TRANSIT", transitRes.getDispatchStatus());

        // Verify LoadingActivity and LoadingOrder status updated to IN_TRANSIT
        LoadingActivity transitActivity = loadingActivityRepository.findById(activity.getId()).orElseThrow();
        Assertions.assertEquals(LoadingActivityStatus.IN_TRANSIT, transitActivity.getStatus());

        LoadingOrder transitOrder = loadingOrderRepository.findById(loadingOrder.getId()).orElseThrow();
        Assertions.assertEquals(LoadingOrderStatus.IN_TRANSIT, transitOrder.getStatus());
    }
}
