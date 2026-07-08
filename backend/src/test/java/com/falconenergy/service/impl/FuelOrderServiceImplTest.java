package com.falconenergy.service.impl;

import com.falconenergy.entity.Customer;
import com.falconenergy.entity.FuelOrder;
import com.falconenergy.entity.FuelProduct;
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
        when(invoiceRepository.save(any(com.falconenergy.entity.Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("sales-agent", "password", java.util.List.of())
        );

        fuelOrderService.updateOrderStatus(30L, "APPROVED");

        verify(auditLogService).log(
                any(String.class),
                any(String.class),
                any(Long.class),
                any(String.class),
                contains("sales-agent")
        );
    }
}
