package com.falconenergy.service.impl;

import com.falconenergy.entity.Customer;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.FuelProduct;
import com.falconenergy.entity.CompanySettings;
import com.falconenergy.dto.FuelOrderRequest;
import com.falconenergy.dto.FuelOrderResponse;
import com.falconenergy.dto.RouteResult;
import com.falconenergy.mapper.FuelOrderMapper;
import com.falconenergy.repository.CustomerRepository;
import com.falconenergy.repository.FuelOrderRepository;
import com.falconenergy.repository.FuelProductRepository;
import com.falconenergy.repository.FuelTransactionRepository;
import com.falconenergy.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FuelOrderServiceImplTest {

    @Mock
    private FuelOrderRepository fuelOrderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private FuelProductRepository fuelProductRepository;

    @Mock
    private FuelOrderMapper fuelOrderMapper;

    @Mock
    private FuelTransactionRepository fuelTransactionRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private com.falconenergy.repository.StorageTankRepository storageTankRepository;

    @Mock
    private com.falconenergy.service.FuelTransactionService fuelTransactionService;

    @Mock
    private com.falconenergy.service.StorageTankService storageTankService;

    @Mock
    private com.falconenergy.repository.InvoiceRepository invoiceRepository;

    @Mock
    private com.falconenergy.repository.PaymentAccountRepository paymentAccountRepository;

    @Mock
    private com.falconenergy.repository.UserRepository userRepository;

    @Mock
    private com.falconenergy.service.SystemSettingService systemSettingService;

    @Mock private com.falconenergy.service.FleetAllocationService fleetAllocationService;
    @Mock private com.falconenergy.repository.OrderTruckAllocationRepository orderTruckAllocationRepository;
    @Mock private com.falconenergy.repository.TruckPricingRepository truckPricingRepository;
    @Mock private com.falconenergy.repository.VehicleRepository vehicleRepository;
    @Mock private com.falconenergy.service.FuelPriceRangeService fuelPriceRangeService;
    @Mock private com.falconenergy.service.TransportPriceRangeService transportPriceRangeService;
    @Mock private com.falconenergy.service.RoutingService routingService;
    @Mock private com.falconenergy.service.TransportPricingService transportPricingService;
    @Mock private com.falconenergy.repository.CompanySettingsRepository companySettingsRepository;

    @InjectMocks
    private FuelOrderServiceImpl fuelOrderService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateOrderStatus_Approved_IncludesAuthenticatedUserInAuditTrail() {
        Customer customer = Customer.builder().id(10L).customerCode("CUST-001").build();
        FuelProduct product = FuelProduct.builder()
                .id(20L)
                .productName("Diesel")
                .fuelType("AGO")
                .unitPrice(new BigDecimal("3200"))
                .density(new BigDecimal("0.85"))
                .availableQuantity(new BigDecimal("1000"))
                .status("ACTIVE")
                .build();

        FuelOrder order = FuelOrder.builder()
                .id(30L)
                .orderNumber("ORD-001")
                .customer(customer)
                .product(product)
                .quantity(new BigDecimal("100"))
                .amount(new BigDecimal("320000"))
                .orderStatus("PENDING")
                .build();

        when(fuelOrderRepository.findById(30L)).thenReturn(Optional.of(order));
        when(fuelOrderRepository.save(any(FuelOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fuelProductRepository.findById(20L)).thenReturn(Optional.of(product));
        when(fuelPriceRangeService.resolvePrice(product, new BigDecimal("100"))).thenReturn(new BigDecimal("3200"));
        com.falconenergy.entity.PaymentAccount paymentAccount = com.falconenergy.entity.PaymentAccount.builder()
                .id(1L)
                .paymentMethod("Bank Transfer")
                .currency("USD")
                .status("ACTIVE")
                .validityDays(30)
                .build();
        when(paymentAccountRepository.findByStatus("ACTIVE")).thenReturn(java.util.List.of(paymentAccount));
        when(invoiceRepository.save(any(com.falconenergy.entity.Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemSettingService.getSetting("INVOICE_PREFIX", "INV-")).thenReturn("INV-");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("sales-agent", "password", java.util.List.of())
        );

        fuelOrderService.updateOrderStatus(30L, "SALES_CONFIRMED");

        verify(auditLogService).log(
                any(String.class),
                any(String.class),
                any(Long.class),
                any(String.class),
                contains("sales-agent")
        );
    }

    @Test
    void mappedDeliveryReplacesLitreFallbackWithOneDistanceTransportCharge() {
        Customer customer = Customer.builder().id(10L).customerCode("CUST-001").build();
        FuelProduct product = FuelProduct.builder().id(20L).productName("Diesel").currency("TZS").build();
        FuelOrder mappedOrder = FuelOrder.builder().build();
        FuelOrderRequest request = FuelOrderRequest.builder()
                .orderNumber("ORD-MAPPED-001").customerId(10L).productId(20L).quantity(new BigDecimal("800"))
                .locationAddress("Customer location").deliveryLatitude(new BigDecimal("-6.8000000"))
                .deliveryLongitude(new BigDecimal("39.2500000")).transportCharges(new BigDecimal("1")).build();

        when(fuelOrderRepository.existsByOrderNumber(request.getOrderNumber())).thenReturn(false);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(fuelProductRepository.findById(20L)).thenReturn(Optional.of(product));
        when(fuelPriceRangeService.resolvePrice(product, request.getQuantity())).thenReturn(new BigDecimal("3200"));
        when(transportPriceRangeService.resolve(product, request.getQuantity())).thenReturn(new BigDecimal("30000"));
        when(fuelOrderMapper.toEntity(request)).thenReturn(mappedOrder);
        when(companySettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(CompanySettings.builder()
                .depotLatitude(new BigDecimal("-6.7923540")).depotLongitude(new BigDecimal("39.2083280")).build()));
        when(routingService.calculateDrivingRoute(any(), any(), any(), any())).thenReturn(RouteResult.builder()
                .distanceKm(new BigDecimal("17.4")).durationSeconds(1200L).provider("OSRM").routeType("SHORTEST_AVAILABLE").build());
        when(transportPricingService.resolveDistancePrice(new BigDecimal("17.4"))).thenReturn(new BigDecimal("45000"));
        when(fuelOrderRepository.save(any(FuelOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fuelOrderMapper.toResponse(any(FuelOrder.class))).thenReturn(FuelOrderResponse.builder().build());

        fuelOrderService.createOrder(request);

        assertEquals(new BigDecimal("45000"), mappedOrder.getTransportCharges());
        assertEquals(BigDecimal.ZERO, mappedOrder.getDistanceTransportPrice());
        assertEquals(new BigDecimal("17.4"), mappedOrder.getDeliveryDistanceKm());
    }
}
