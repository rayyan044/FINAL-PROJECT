package com.falconenergy.service.impl;

import com.falconenergy.dto.DeliveryNoteResponse;
import com.falconenergy.dto.TruckInvoiceResponse;
import com.falconenergy.entity.*;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.repository.*;
import com.falconenergy.service.DeliveryDocumentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest
public class DeliveryDocumentServiceTest {

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

    @Test
    @Transactional
    @WithMockUser(username = "ops_officer", authorities = {"ROLE_OPERATIONS"})
    public void testDeliveryDocumentWorkflow() {
        // 1. Setup metadata
        Customer customer = customerRepository.save(Customer.builder()
                .companyName("Operations Test Customer")
                .customerCode("CUST-OPS")
                .contactPerson("Test Contact")
                .email("ops@customer.com")
                .phone("1234567")
                .status("ACTIVE")
                .build());

        FuelProduct product = productRepository.save(FuelProduct.builder()
                .productName("OPS AGO")
                .fuelType("AGO")
                .unitPrice(new BigDecimal("1.50"))
                .density(new BigDecimal("0.8400"))
                .availableQuantity(new BigDecimal("50000.00"))
                .status("ACTIVE")
                .currency("USD")
                .build());

        FuelOrder order = fuelOrderRepository.save(FuelOrder.builder()
                .orderNumber("ORD-OPS-001")
                .customer(customer)
                .product(product)
                .quantity(new BigDecimal("10000.00"))
                .amount(new BigDecimal("15000.00"))
                .orderStatus("APPROVED")
                .build());

        LoadingOrder loadingOrder = loadingOrderRepository.save(LoadingOrder.builder()
                .loadingOrderNumber("LO-OPS-001")
                .order(order)
                .loadingDate(LocalDate.now())
                .loadingTerminal("Ops Terminal")
                .consignee("Ops Consignee")
                .status(LoadingOrderStatus.LOADING_IN_PROGRESS)
                .build());

        LoadingActivity activity = loadingActivityRepository.save(LoadingActivity.builder()
                .loadingOrder(loadingOrder)
                .truckNumber("T-OPS-111")
                .driverName("John Ops")
                .driverLicenceNumber("LIC-OPS-111")
                .transportCompany("Ops Transport")
                .destination("Ops Yard")
                .product("AGO")
                .allocatedQuantity(new BigDecimal("10000.00"))
                .queueNumber("Q1")
                .bayNumber("BAY1")
                .status(LoadingActivityStatus.PENDING)
                .build());

        // Test check: Cannot generate delivery note before loading is COMPLETED
        Assertions.assertThrows(BadRequestException.class, () -> {
            deliveryDocumentService.generateDeliveryNote(activity.getId());
        });

        // 2. Complete loading activity
        activity.setStatus(LoadingActivityStatus.COMPLETED);
        activity.setAmbientVolume(new BigDecimal("10000.00"));
        activity.setStandardVolume(new BigDecimal("9950.00"));
        activity.setCompletedAt(LocalDateTime.now());
        loadingActivityRepository.save(activity);

        // Test check: Cannot generate delivery note before Loading Report is created
        Assertions.assertThrows(BadRequestException.class, () -> {
            deliveryDocumentService.generateDeliveryNote(activity.getId());
        });

        // 3. Create Loading Report
        loadingReportRepository.save(LoadingReport.builder()
                .loadingActivity(activity)
                .loadingOrder(loadingOrder)
                .reportNumber("REP-OPS-001")
                .loadingOfficer("ops_officer")
                .terminal("Ops Terminal")
                .loadingBay("BAY1")
                .reportStatus(LoadingReportStatus.GENERATED)
                .build());

        // 4. Generate Delivery Note
        DeliveryNoteResponse dnResponse = deliveryDocumentService.generateDeliveryNote(activity.getId());
        Assertions.assertNotNull(dnResponse);
        Assertions.assertEquals("PREPARED", dnResponse.getStatus());
        Assertions.assertEquals("T-OPS-111", dnResponse.getTruckNumber());
        Assertions.assertEquals(0, new BigDecimal("9950.00").compareTo(dnResponse.getStandardVolume()));
        Assertions.assertTrue(dnResponse.getDeliveryNoteNumber().startsWith("DN-"));

        // Verify duplicate prevention for Delivery Note
        Assertions.assertThrows(BadRequestException.class, () -> {
            deliveryDocumentService.generateDeliveryNote(activity.getId());
        });

        // Verify duplicate prevention for Truck Invoice (fails because Delivery Note must exist first - it does, but we shouldn't run duplicates later)
        // Let's generate the Invoice first
        TruckInvoiceResponse invoiceResponse = deliveryDocumentService.generateTruckInvoice(activity.getId());
        Assertions.assertNotNull(invoiceResponse);
        Assertions.assertEquals("GENERATED", invoiceResponse.getInvoiceStatus());
        Assertions.assertEquals(0, new BigDecimal("1.50").compareTo(invoiceResponse.getUnitPrice()));
        // Quantity standard volume 9950 * price 1.50 = 14925.00
        Assertions.assertEquals(0, new BigDecimal("14925.00").compareTo(invoiceResponse.getTotalAmount()));
        Assertions.assertTrue(invoiceResponse.getInvoiceNumber().startsWith("INV-TRUCK-"));

        // Verify duplicate prevention for Truck Invoice
        Assertions.assertThrows(BadRequestException.class, () -> {
            deliveryDocumentService.generateTruckInvoice(activity.getId());
        });

        // Verify state is DOCUMENTS_READY
        LoadingOrder updatedLO = loadingOrderRepository.findById(loadingOrder.getId()).orElseThrow();
        Assertions.assertEquals(LoadingOrderStatus.DOCUMENTS_READY, updatedLO.getStatus());

        // 5. Print Documents
        DeliveryNoteResponse printedDN = deliveryDocumentService.printDeliveryNote(dnResponse.getId());
        Assertions.assertEquals("PRINTED", printedDN.getStatus());

        TruckInvoiceResponse printedInvoice = deliveryDocumentService.printTruckInvoice(invoiceResponse.getId());
        Assertions.assertEquals("PRINTED", printedInvoice.getInvoiceStatus());

        // 6. Handover to driver
        DeliveryNoteResponse handedDN = deliveryDocumentService.markHandedToDriver(dnResponse.getId());
        Assertions.assertEquals("HANDED_TO_DRIVER", handedDN.getStatus());

        // Verify state transitions to READY_FOR_DISPATCH
        LoadingOrder finalLO = loadingOrderRepository.findById(loadingOrder.getId()).orElseThrow();
        Assertions.assertEquals(LoadingOrderStatus.READY_FOR_DISPATCH, finalLO.getStatus());
    }
}
