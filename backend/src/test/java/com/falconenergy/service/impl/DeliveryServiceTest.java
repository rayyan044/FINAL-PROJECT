package com.falconenergy.service.impl;

import com.falconenergy.dto.*;
import com.falconenergy.entity.*;
import com.falconenergy.exception.BadRequestException;
import com.falconenergy.repository.*;
import com.falconenergy.service.DeliveryDocumentService;
import com.falconenergy.service.DispatchService;
import com.falconenergy.service.DeliveryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class DeliveryServiceTest {

    @Autowired
    private DeliveryService deliveryService;

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

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentReceiptRepository paymentReceiptRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    @WithMockUser(username = "delivery_officer", authorities = {"ROLE_DISPATCHER", "ROLE_OPERATIONS"})
    public void testDeliveryManagementWorkflow() {
        // 1. Setup metadata
        Customer customer = customerRepository.save(Customer.builder()
                .companyName("Delivery E2E Corp")
                .customerCode("CUST-DEL")
                .contactPerson("Alice Delivery")
                .email("alice@del.com")
                .phone("11223344")
                .status("ACTIVE")
                .build());

        FuelProduct product = productRepository.save(FuelProduct.builder()
                .productName("DEL AGO")
                .fuelType("AGO")
                .unitPrice(new BigDecimal("1.70"))
                .density(new BigDecimal("0.8400"))
                .availableQuantity(new BigDecimal("80000.00"))
                .status("ACTIVE")
                .currency("USD")
                .build());

        Driver driver = driverRepository.save(Driver.builder()
                .firstName("Dan")
                .lastName("Driver")
                .phone("255700000777")
                .licenseNumber("LIC-DEL-777")
                .status("AVAILABLE")
                .build());
        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .truckNumber("T-DEL-777")
                .plateNumber("DEL-777")
                .capacity(new BigDecimal("20000"))
                .currentStatus("ASSIGNED")
                .active(true)
                .driver(driver)
                .assignedFuelTypes(new java.util.HashSet<>(java.util.Set.of("AGO")))
                .build());

        FuelOrder order = fuelOrderRepository.save(FuelOrder.builder()
                .orderNumber("ORD-DEL-001")
                .customer(customer)
                .product(product)
                .quantity(new BigDecimal("10000.00"))
                .amount(new BigDecimal("17000.00"))
                .orderStatus("APPROVED")
                .build());

        LoadingOrder loadingOrder = loadingOrderRepository.save(LoadingOrder.builder()
                .loadingOrderNumber("LO-DEL-001")
                .order(order)
                .loadingDate(LocalDate.now())
                .loadingTerminal("East Terminal")
                .consignee("Del Consignee")
                .status(LoadingOrderStatus.LOADING_IN_PROGRESS)
                .build());

        LoadingActivity activity = loadingActivityRepository.save(LoadingActivity.builder()
                .loadingOrder(loadingOrder)
                .vehicle(vehicle)
                .truckNumber("T-DEL-777")
                .driverName("Dan Driver")
                .driverLicenceNumber("LIC-DEL-777")
                .transportCompany("Del Transport")
                .destination("Alice Fuel Station")
                .product("AGO")
                .allocatedQuantity(new BigDecimal("10000.00"))
                .queueNumber("Q7")
                .bayNumber("BAY3")
                .status(LoadingActivityStatus.COMPLETED)
                .ambientVolume(new BigDecimal("10000.00"))
                .standardVolume(new BigDecimal("9950.00"))
                .completedAt(LocalDateTime.now())
                .build());

        LoadingReport report = loadingReportRepository.save(LoadingReport.builder()
                .loadingActivity(activity)
                .loadingOrder(loadingOrder)
                .reportNumber("REP-DEL-001")
                .loadingOfficer("delivery_officer")
                .terminal("East Terminal")
                .loadingBay("BAY3")
                .reportStatus(LoadingReportStatus.GENERATED)
                .build());

        // Create Delivery Note and Truck Invoice
        DeliveryNoteResponse dnResponse = deliveryDocumentService.generateDeliveryNote(activity.getId());
        TruckInvoiceResponse invoiceResponse = deliveryDocumentService.generateTruckInvoice(activity.getId());

        // Print and Handover documents
        deliveryDocumentService.printDeliveryNote(dnResponse.getId());
        deliveryDocumentService.printTruckInvoice(invoiceResponse.getId());
        deliveryDocumentService.markHandedToDriver(dnResponse.getId());

        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceNumber("INV-DEL-001")
                .invoiceDate(LocalDateTime.now())
                .order(order)
                .subtotal(new BigDecimal("17000.00"))
                .tax(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("17000.00"))
                .paymentStatus("PAID")
                .financeApprovedBy("delivery_officer")
                .financeApprovedAt(LocalDateTime.now())
                .build());
        order.setInvoice(invoice);
        fuelOrderRepository.save(order);
        paymentReceiptRepository.save(PaymentReceipt.builder()
                .receiptNumber("PR-DEL-001")
                .invoice(invoice)
                .receiptStatus("PAID")
                .receivedAmount(invoice.getGrandTotal())
                .receivedAt(LocalDateTime.now())
                .confirmedBy("delivery_officer")
                .build());
        entityManager.flush();
        entityManager.refresh(loadingOrder);

        // Create Dispatch
        DispatchResponse dispatchResponse = dispatchService.createDispatch(activity.getId(), null);

        // Release Truck
        dispatchService.releaseTruck(dispatchResponse.getId());

        // 2. Start Transit - This should automatically trigger Delivery creation!
        DispatchResponse transitDispatch = dispatchService.startTransit(dispatchResponse.getId());
        Assertions.assertEquals("IN_TRANSIT", transitDispatch.getDispatchStatus());

        // Assert: Verify Delivery is automatically created in IN_TRANSIT
        Delivery delivery = deliveryRepository.findByDispatchId(dispatchResponse.getId())
                .orElseThrow(() -> new AssertionError("Delivery record should have been auto-created."));
        Assertions.assertEquals(DeliveryStatus.IN_TRANSIT, delivery.getDeliveryStatus());
        Assertions.assertTrue(delivery.getDeliveryNumber().startsWith("DEL-"));
        Assertions.assertEquals("T-DEL-777", delivery.getTruckNumber());

        // Assert: Duplicate prevention (returns existing instead of creating duplicate)
        DeliveryResponse secondaryResponse = deliveryService.createDelivery(dispatchResponse.getId());
        Assertions.assertEquals(delivery.getId(), secondaryResponse.getId());

        // Try to complete delivery before arrival (should fail)
        Assertions.assertThrows(BadRequestException.class, () -> {
            deliveryService.completeDelivery(delivery.getId(), null);
        });

        // 3. Record Arrival
        DeliveryResponse arrivedRes = deliveryService.markArrived(delivery.getId(),
                DeliveryArrivalRequest.builder().receivedBy("Security Guard").remarks("Arrived on schedule").build());
        Assertions.assertEquals("ARRIVED_AT_DESTINATION", arrivedRes.getDeliveryStatus());
        Assertions.assertEquals("Security Guard", arrivedRes.getReceivedBy());

        // Assert invalid transition: cannot mark arrival when already ARRIVED
        Assertions.assertThrows(BadRequestException.class, () -> {
            deliveryService.markArrived(delivery.getId(), null);
        });

        // 4. Complete Delivery
        DeliveryResponse deliveredRes = deliveryService.completeDelivery(delivery.getId(),
                DeliveryCompleteRequest.builder().completedBy("Alice Receiver").remarks("Quantity matches perfectly").build());
        Assertions.assertEquals("DELIVERED", deliveredRes.getDeliveryStatus());
        Assertions.assertEquals("Alice Receiver", deliveredRes.getCompletedBy());

        // Verify LoadingActivity status updated to DELIVERED
        LoadingActivity updatedActivity = loadingActivityRepository.findById(activity.getId()).orElseThrow();
        Assertions.assertEquals(LoadingActivityStatus.DELIVERED, updatedActivity.getStatus());

        // Verify LoadingOrder status updated to DELIVERED
        LoadingOrder updatedOrder = loadingOrderRepository.findById(loadingOrder.getId()).orElseThrow();
        Assertions.assertEquals(LoadingOrderStatus.DELIVERED, updatedOrder.getStatus());
    }
}
